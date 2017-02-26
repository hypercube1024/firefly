package com.firefly.server.http2.router.handler.file;

import com.firefly.codec.http2.model.*;
import com.firefly.net.buffer.FileRegion;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandler;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.URIUtils;

import java.io.*;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class StaticFileHandler extends DefaultErrorResponseHandler {

    private StaticFileConfiguration configuration;

    public StaticFileHandler(StaticFileConfiguration configuration) {
        this.configuration = configuration;
    }

    public StaticFileHandler(String rootPath) {
        this(new StaticFileConfiguration());
        configuration.setRootPath(rootPath);
    }

    @Override
    public void handle(RoutingContext ctx) {
        File file = new File(configuration.getRootPath(), URIUtils.canonicalPath(ctx.getURI().getPath()));
        if (file.exists()) {
            long contentLength = file.length();
            String mimetype = MimeTypes.getDefaultMimeByExtension(file.getName());

            List<String> reqRanges = ctx.getFields().getValuesList(HttpHeader.RANGE.asString());
            if (reqRanges == null || reqRanges.isEmpty()) {
                ctx.setStatus(HttpStatus.OK_200);
                ctx.put(HttpHeader.CONTENT_LENGTH, String.valueOf(contentLength));
                if (StringUtils.hasText(mimetype)) {
                    ctx.put(HttpHeader.CONTENT_TYPE, mimetype);
                }

                try (OutputStream out = ctx.getResponse().getOutputStream();
                     BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    IO.copy(in, out, contentLength);
                } catch (FileNotFoundException e) {
                    render(ctx, HttpStatus.NOT_FOUND_404, null);
                } catch (IOException e) {
                    if (ctx.getResponse().isCommited()) {
                        render(ctx, HttpStatus.INTERNAL_SERVER_ERROR_500, e);
                    }
                }
            } else {
                // Parse the satisfiable ranges
                List<InclusiveByteRange> ranges = InclusiveByteRange.satisfiableRanges(reqRanges, contentLength);

                if (ranges == null || ranges.size() == 0) {
                    // if there are no satisfiable ranges, send 416 response
                    ctx.put(HttpHeader.CONTENT_RANGE, InclusiveByteRange.to416HeaderRangeString(contentLength));
                    render(ctx, HttpStatus.RANGE_NOT_SATISFIABLE_416, null);
                } else {
                    //  if there is only a single valid range (must be satisfiable
                    //  since were here now), send that range with a 206 response
                    if (ranges.size() == 1) {
                        InclusiveByteRange singleSatisfiableRange = ranges.get(0);
                        long singleLength = singleSatisfiableRange.getSize(contentLength);
                        ctx.setStatus(HttpStatus.PARTIAL_CONTENT_206);
                        ctx.put(HttpHeader.CONTENT_LENGTH, String.valueOf(singleLength));
                        ctx.put(HttpHeader.CONTENT_RANGE, singleSatisfiableRange.toHeaderRangeString(contentLength));
                        if (StringUtils.hasText(mimetype)) {
                            ctx.put(HttpHeader.CONTENT_TYPE, mimetype);
                        }

                        long position = singleSatisfiableRange.getFirst(contentLength);
                        try (FileRegion fileRegion = new FileRegion(file, position, singleLength);
                             OutputStream out = ctx.getResponse().getOutputStream()) {
                            fileRegion.transferTo(Callback.NOOP, (buf, callback, count) -> out.write(BufferUtils.toArray(buf)));
                        } catch (FileNotFoundException e) {
                            render(ctx, HttpStatus.NOT_FOUND_404, null);
                        } catch (IOException e) {
                            if (ctx.getResponse().isCommited()) {
                                render(ctx, HttpStatus.INTERNAL_SERVER_ERROR_500, e);
                            }
                        }
                    } else {
                        //  multiple non-overlapping valid ranges cause a multipart
                        //  206 response which does not require an overall content-length header
                        ctx.setStatus(HttpStatus.PARTIAL_CONTENT_206);

                        InputStream in = null;
                        try (MultiPartOutputStream multi = new MultiPartOutputStream(ctx.getResponse().getOutputStream())) {
                            String ctp;
                            if (ctx.getFields().get(HttpHeader.REQUEST_RANGE) != null) {
                                ctp = "multipart/x-byteranges; boundary=";
                            } else {
                                ctp = "multipart/byteranges; boundary=";
                            }
                            ctx.put(HttpHeader.CONTENT_TYPE, ctp + multi.getBoundary());
                            in = new BufferedInputStream(new FileInputStream(file));
                            long pos = 0;


                            // calculate the content-length
                            int length = 0;
                            String[] header = new String[ranges.size()];
                            for (int i = 0; i < ranges.size(); i++) {
                                InclusiveByteRange ibr = ranges.get(i);
                                header[i] = ibr.toHeaderRangeString(contentLength);
                                length += ((i > 0) ? 2 : 0) +
                                        2 + multi.getBoundary().length() + 2 +
                                        (mimetype == null ? 0 : HttpHeader.CONTENT_TYPE.asString().length() + 2 + mimetype.length()) + 2 +
                                        HttpHeader.CONTENT_RANGE.asString().length() + 2 + header[i].length() + 2 +
                                        2 +
                                        (ibr.getLast(contentLength) - ibr.getFirst(contentLength)) + 1;
                            }
                            length += 2 + 2 + multi.getBoundary().length() + 2 + 2;
                            ctx.put(HttpHeader.CONTENT_LENGTH, String.valueOf(length));

                            for (int i = 0; i < ranges.size(); i++) {
                                InclusiveByteRange ibr = ranges.get(i);
                                multi.startPart(mimetype, new String[]{HttpHeader.CONTENT_RANGE + ": " + header[i]});

                                long start = ibr.getFirst(contentLength);
                                long size = ibr.getSize(contentLength);

                                // Handle non cached resource
                                if (start < pos) {
                                    in.close();
                                    in = new BufferedInputStream(new FileInputStream(file));
                                    pos = 0;
                                }
                                if (pos < start) {
                                    in.skip(start - pos);
                                    pos = start;
                                }

                                IO.copy(in, multi, size);
                                pos += size;
                            }
                            in.close();
                        } catch (IOException e) {
                            if (in != null) {
                                IO.close(in);
                            }
                            if (ctx.getResponse().isCommited()) {
                                render(ctx, HttpStatus.INTERNAL_SERVER_ERROR_500, e);
                            }
                        }
                    }
                }
            }
        } else {
            render(ctx, HttpStatus.NOT_FOUND_404, null);
        }
    }

}

package com.firefly.client.websocket;

import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.websocket.model.ExtensionConfig;
import com.firefly.codec.websocket.model.UpgradeResponseAdapter;

import java.util.List;

public class ClientUpgradeResponse extends UpgradeResponseAdapter {
    private List<ExtensionConfig> extensions;

    public ClientUpgradeResponse() {
        super();
    }

    public ClientUpgradeResponse(MetaData.Response response) {
        super();
        setStatusCode(response.getStatus());
        setStatusReason(response.getReason());

        HttpFields fields = response.getFields();
        for (HttpField field : fields) {
            addHeader(field.getName(), field.getValue());
        }

        HttpField extensionsField = fields.getField(HttpHeader.SEC_WEBSOCKET_EXTENSIONS);
        if (extensionsField != null) {
            this.extensions = ExtensionConfig.parseList(extensionsField.getValues());
        }
        setAcceptedSubProtocol(fields.get(HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL));
    }

    @Override
    public List<ExtensionConfig> getExtensions() {
        return this.extensions;
    }

    @Override
    public void sendForbidden(String message) {
        throw new UnsupportedOperationException("Not supported on client implementation");
    }
}

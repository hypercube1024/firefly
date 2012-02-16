package com.firefly.utils.io;

import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

public class BufferedRandomAccessFile extends RandomAccessFile {

	private static final int DEFAULT_BUFFER_BIT_LEN = 10;

	protected byte buf[];
	protected int bufbitlen;
	protected int bufsize;
	protected long bufmask;
	protected boolean bufdirty;
	protected int bufusedsize;
	protected long curpos;

	protected long bufstartpos;
	protected long bufendpos;
	protected long fileendpos;

	protected boolean append;
	protected String filename;
	protected long initfilelen;

	public BufferedRandomAccessFile(String name) throws IOException {
		this(name, "r", DEFAULT_BUFFER_BIT_LEN);
	}

	public BufferedRandomAccessFile(File file) throws IOException,
			FileNotFoundException {
		this(file.getPath(), "r", DEFAULT_BUFFER_BIT_LEN);
	}

	public BufferedRandomAccessFile(String name, int bufbitlen)
			throws IOException {
		this(name, "r", bufbitlen);
	}

	public BufferedRandomAccessFile(File file, int bufbitlen)
			throws IOException, FileNotFoundException {
		this(file.getPath(), "r", bufbitlen);
	}

	public BufferedRandomAccessFile(String name, String mode)
			throws IOException {
		this(name, mode, DEFAULT_BUFFER_BIT_LEN);
	}

	public BufferedRandomAccessFile(File file, String mode) throws IOException,
			FileNotFoundException {
		this(file.getPath(), mode, DEFAULT_BUFFER_BIT_LEN);
	}

	public BufferedRandomAccessFile(String name, String mode, int bufbitlen)
			throws IOException {
		super(name, mode);
		init(name, mode, bufbitlen);
	}

	public BufferedRandomAccessFile(File file, String mode, int bufbitlen)
			throws IOException, FileNotFoundException {
		this(file.getPath(), mode, bufbitlen);
	}

	private void init(String name, String mode, int bufbitlen)
			throws IOException {
		if (mode.equals("r") == true) {
			append = false;
		} else {
			append = true;
		}

		filename = name;
		initfilelen = super.length();
		fileendpos = initfilelen - 1;
		curpos = super.getFilePointer();

		if (bufbitlen < 0) {
			throw new IllegalArgumentException("bufbitlen size must >= 0");
		}

		this.bufbitlen = bufbitlen;
		bufsize = 1 << bufbitlen;
		buf = new byte[bufsize];
		bufmask = ~((long) bufsize - 1L);
		bufdirty = false;
		bufusedsize = 0;
		bufstartpos = -1;
		bufendpos = -1;
	}

	private void flushbuf() throws IOException {
		if (bufdirty == true) {
			if (super.getFilePointer() != bufstartpos) {
				super.seek(bufstartpos);
			}
			super.write(buf, 0, bufusedsize);
			bufdirty = false;
		}
	}

	private int fillbuf() throws IOException {
		super.seek(bufstartpos);
		bufdirty = false;
		return super.read(buf);
	}

	public byte read(long pos) throws IOException {
		if (pos < bufstartpos || pos > bufendpos) {
			flushbuf();
			seek(pos);

			if ((pos < bufstartpos) || (pos > bufendpos)) {
				throw new IOException();
			}
		}
		curpos = pos;
		return buf[(int) (pos - bufstartpos)];
	}

	public boolean write(byte bw) throws IOException {
		return write(bw, curpos);
	}

	public boolean append(byte bw) throws IOException {
		return write(bw, fileendpos + 1);
	}

	public boolean write(byte bw, long pos) throws IOException {

		if ((pos >= bufstartpos) && (pos <= bufendpos)) {
			buf[(int) (pos - bufstartpos)] = bw;
			bufdirty = true;

			if (pos == fileendpos + 1) { // write pos is append pos
				fileendpos++;
				bufusedsize++;
			}
		} else { // write pos not in buf
			seek(pos);

			if ((pos >= 0) && (pos <= fileendpos) && (fileendpos != 0)) {
				buf[(int) (pos - bufstartpos)] = bw;
			} else if (((pos == 0) && (fileendpos == 0))
					|| (pos == fileendpos + 1)) { // write pos is append pos
				buf[0] = bw;
				fileendpos++;
				bufusedsize = 1;
			} else {
				throw new IndexOutOfBoundsException();
			}
			bufdirty = true;
		}
		curpos = pos;
		return true;
	}

	public void write(byte b[], int off, int len) throws IOException {

		long writeendpos = curpos + len - 1;

		if (writeendpos <= bufendpos) { // b[] in cur buf
			System.arraycopy(b, off, buf, (int) (curpos - bufstartpos), len);
			bufdirty = true;
			bufusedsize = (int) (writeendpos - bufstartpos + 1);
		} else { // b[] not in cur buf
			super.seek(curpos);
			super.write(b, off, len);
		}

		if (writeendpos > fileendpos)
			fileendpos = writeendpos;

		seek(writeendpos + 1);
	}

	public int read(byte b[], int off, int len) throws IOException {

		long readendpos = curpos + len - 1;

		if (readendpos <= bufendpos && readendpos <= fileendpos) {
			System.arraycopy(buf, (int) (curpos - bufstartpos), b, off, len);
		} else { // read b[] size > buf[]
			if (readendpos > fileendpos) { // read b[] part in file
				len = (int) (this.length() - curpos + 1);
			}

			super.seek(curpos);
			len = super.read(b, off, len);
			readendpos = curpos + len - 1;
		}
		seek(readendpos + 1);
		return len;
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	public void seek(long pos) throws IOException {

		if ((pos < bufstartpos) || (pos > bufendpos)) { // seek pos not in buf
			this.flushbuf();
			if ((pos >= 0) && (pos <= fileendpos) && (fileendpos != 0)) {
				bufstartpos = pos & bufmask;
				bufusedsize = fillbuf();

			} else if (((pos == 0) && (fileendpos == 0))
					|| (pos == fileendpos + 1)) { // seek pos is append pos
				bufstartpos = pos;
				bufusedsize = 0;
			}
			bufendpos = bufstartpos + bufsize - 1;
		}
		curpos = pos;
	}

	public long length() throws IOException {
		return this.max(fileendpos + 1, initfilelen);
	}

	public void setLength(long newLength) throws IOException {
		if (newLength > 0) {
			fileendpos = newLength - 1;
		} else {
			fileendpos = 0;
		}
		super.setLength(newLength);
	}

	public long getFilePointer() throws IOException {
		return curpos;
	}

	private long max(long a, long b) {
		if (a > b)
			return a;
		return b;
	}

	public void close() throws IOException {
		this.flushbuf();
		super.close();
	}

	public static void main(String[] args) throws IOException {
		long readfilelen = 0;
		BufferedRandomAccessFile brafReadFile, brafWriteFile;

		brafReadFile = new BufferedRandomAccessFile(
				"C:/Windows/Fonts/STKAITI.TTF");
		readfilelen = brafReadFile.initfilelen;
		brafWriteFile = new BufferedRandomAccessFile("D:/STKAITI.001", "rw", 10);

		byte buf[] = new byte[1024];
		int readcount;

		long start = System.currentTimeMillis();

		while ((readcount = brafReadFile.read(buf)) != -1) {
			brafWriteFile.write(buf, 0, readcount);
		}

		brafWriteFile.close();
		brafReadFile.close();

		System.out.println("BufferedRandomAccessFile Copy & Write File: "
				+ brafReadFile.filename + "    FileSize: "
				+ java.lang.Integer.toString((int) readfilelen >> 1024)
				+ " (KB)    " + "Spend: "
				+ (double) (System.currentTimeMillis() - start) / 1000 + "(s)");

		java.io.FileInputStream fdin = new java.io.FileInputStream(
				"C:/Windows/Fonts/STKAITI.TTF");
		java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fdin,
				1024);
		java.io.DataInputStream dis = new java.io.DataInputStream(bis);

		java.io.FileOutputStream fdout = new java.io.FileOutputStream(
				"D:/STKAITI.002");
		java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(
				fdout, 1024);
		java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);

		start = System.currentTimeMillis();

		for (int i = 0; i < readfilelen; i++) {
			dos.write(dis.readByte());
		}

		dos.close();
		dis.close();

		System.out.println("DataBufferedios Copy & Write File: "
				+ brafReadFile.filename + "    FileSize: "
				+ java.lang.Integer.toString((int) readfilelen >> 1024)
				+ " (KB)    " + "Spend: "
				+ (double) (System.currentTimeMillis() - start) / 1000 + "(s)");
	}
}

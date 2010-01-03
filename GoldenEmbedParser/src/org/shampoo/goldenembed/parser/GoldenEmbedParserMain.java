/*
 * Copyright (c) 2009 Justin F. Knotzke (jknotzke@shampoo.ca)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */


package org.shampoo.goldenembed.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class GoldenEmbedParserMain 
{

	static final byte MESG_RESPONSE_EVENT_ID = 0x40;
	static final byte MESG_CAPABILITIES_ID = 0x54;
	static final byte MESG_BROADCAST_DATA_ID = 0x4E;
	static final byte MESG_TX_SYNC = (byte)0xA4;
	static final byte MESG_CHANNEL_SEARCH_TIMEOUT_ID =0x44;
	static final byte MESG_ASSIGN_CHANNEL_ID = 0x42;
	static final byte MESG_CHANNEL_RADIO_FREQ_ID = 0x45;
    static final byte MESG_CHANNEL_MESG_PERIOD_ID = 0x43;
    static final byte MESG_OPEN_CHANNEL_ID = (byte)0x4B;
    static final byte MESG_CHANNEL_ID_ID = (byte)0x51; 
    static final byte MESG_NETWORK_KEY_ID  = 0x46;
    
    float totalTrans = 0;
    float totalErrors = 0;
    boolean errorFlag = false;
	
	

	int debug = 0;

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		
		//byte checksum =(byte)0xa4 ^ (byte)0x03 ^ (byte)0x40 ^ (byte)0x00 ^ (byte)0x42 ^ (byte)0x00;
		//byte checksum =(byte)0xa4 ^ (byte)0x03 ^ (byte)0x40 ^ (byte)0x00 ^ (byte)0x51 ^ (byte)0x00;
		
		//System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(checksum));
		
		
		new GoldenEmbedParserMain();
	}

	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}

	GoldenEmbedParserMain() {
		// Load up the file
		File file = new File("/Volumes//DATALOGGER//LOG07.txt");
		try {
			ANTrxHandler(getBytesFromFile(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int ANTrxMsg(byte[] rxIN, int i, int size) 
	{
		//System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(rxIN[i]));
			switch (rxIN[i]) 
			{
				case MESG_RESPONSE_EVENT_ID:
					System.out.println("ID: MESG_RESPONSE_EVENT_ID\n");
					i = ANTresponseHandler(rxIN, i, size);
					break;
				case MESG_CAPABILITIES_ID:
					System.out.println("ID: MESG_CAPABILITIES_ID\n");
					i = ANTCfgCapabilties(i, size); // rxBuf[3] .. skip sync, size, msg
					break;
				case MESG_BROADCAST_DATA_ID:
					System.out.println("ID: MESG_BROADCAST_DATA_ID\n");
					i = ANTparseHRM(rxIN, i+3);
					break;
				default:
					//System.out.println("ID: Unknown 0x" + UnicodeFormatter.byteToHex(rxIN[i]));
			}
			return i;
	}

	public byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	private int ANTparseHRM(byte[] msgData, int i) {

		byte aByte;
		int end = i + 8;
		int hrCountFinder = 0;

		for (; i < end; i++) {
			aByte = msgData[i];
			//System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(msgData[i]));
			if (hrCountFinder == 6) { //HR is the sixth byte
	//			System.out.println("Converting 0x"+ UnicodeFormatter.byteToHex(msgData[i]));
				int hr = unsignedByteToInt(aByte);
				System.out.println("Heart Rate is: " + hr);
			}
			 //else
			 //    System.out.println("o" + i + "=" + unsignedByteToInt(aByte));
			hrCountFinder++;
		}

		return --i; //For Loop will advance itself.
	}

	private void ANTrxHandler(byte[] rxBuf )
	{
		int msgN = 0;
		int i;
		int size = 0;
		boolean inMsg = true;
		
		for (i = 0; i < rxBuf.length; i++)
	    {
			//System.out.println("0x"+ UnicodeFormatter.byteToHex(rxBuf[i]));
			
	    	if (rxBuf[i] == MESG_TX_SYNC && inMsg)
	        {
	    		    inMsg = false;
	        		msgN = 0; // Always reset msg count if we get a sync
	                msgN++;
	                errorFlag = false;
	                totalTrans++;
	                //System.out.println("RX: [sync]");
	         }
	         else if (msgN == 1)
	         {
	        	 Byte aByte = new Byte(rxBuf[i]);
	             msgN++; //Size
	             size = aByte.intValue();
	         }
	         else
	         {
	        	 if(rxBuf.length < size+3+i-2)
	        	 {
	        		 System.out.println("\n\nTotal Errors: " + totalErrors);
	        		 System.out.println("Total Messages " + totalTrans);
	        		 System.out.println("%: " +totalErrors / totalTrans * 100.0);
	        		 System.exit(0); //EOF
	        	 }
	        	 byte checksum = checkSum(rxBuf, size, i-2);
	             if (checksum == rxBuf[size+i+1]) // Check if chksum = msg chksum
	             {
	            	 inMsg = true;
	            	 // Handle Message
	                 i = ANTrxMsg(rxBuf, i, size);
	             }
	             else
	             {
		        	 //System.out.println("CheckSum Mismatch 0x"+ UnicodeFormatter.byteToHex(rxBuf[size+i+1]) + "!=: 0x" + UnicodeFormatter.byteToHex(checksum));
	                 msgN = 0;
	                 inMsg = true; 
	                 if(errorFlag == false)
	                 {
	                	 totalErrors++;
	                	 errorFlag = true;
	                 }
	             } 
	         }
          }
		 System.out.println("\n\nTotal Errors: " + totalErrors);
		 System.out.println("Total Messages " + totalTrans);
		 System.out.println("%: " + (totalErrors / totalTrans) * 100.0);

    }
	
	private byte checkSum(byte data[], int length, int pos)
	{

	        byte chksum = 0x0;

	        for (int i = pos; i < length+3+pos; i++)
	        {  
	        	    //System.out.println("Checksum: 0x"+ UnicodeFormatter.byteToHex(data[i]));
	                chksum ^= data[i];  // +1 since skip prefix sync code, we already counted it
	        }
	        
	        return chksum;
	}
	
	private int ANTresponseHandler(byte rxBuf[], int pos, int size)
	{
		    pos++;
	        byte ch = rxBuf[0+pos];
	        byte id = rxBuf[1+pos];
	        byte code = rxBuf[2+pos];

	        System.out.println("Channel Num:" + UnicodeFormatter.byteToHex(ch));
	        System.out.println("Message ID: " + UnicodeFormatter.byteToHex(id));
	        System.out.println("Code: " + UnicodeFormatter.byteToHex(code));

	        switch (id)
	        {
	                case MESG_CHANNEL_SEARCH_TIMEOUT_ID:
	                        System.out.println("[MESG_CHANNEL_SEARCH_TIMEOUT_ID]\n");
	                        break;
	                case MESG_ASSIGN_CHANNEL_ID :
	                        System.out.println("[MESG_ASSIGN_CHANNEL_ID]\n");
	                        break;
	                case MESG_CHANNEL_RADIO_FREQ_ID :
	                        System.out.println("[MESG_CHANNEL_RADIO_FREQ_ID]\n");
	                        break;
	                case MESG_CHANNEL_MESG_PERIOD_ID :
	                        System.out.println("[MESG_CHANNEL_MESG_PERIOD_ID]\n");
	                        break;
	                case MESG_OPEN_CHANNEL_ID :
	                        System.out.println("[MESG_OPEN_CHANNEL_ID]\n");
	                        break;
	                case MESG_CHANNEL_ID_ID :
	                        System.out.println("[MESG_CHANNEL_ID_ID]\n");
	                        break;
	                case MESG_NETWORK_KEY_ID :
	                        System.out.println("[MESG_NETWORK_KEY_ID]\n");
	                        break;
	                default :
	                        System.out.println("[unknown]\n");
	                        break;
	        }

	        return pos+3; //Read 3 bytes for loop will increment by 1. Move it forward 2
	}
	
	private int ANTCfgCapabilties(int i, int size)
	{
		return i+size+1;
	}	
}

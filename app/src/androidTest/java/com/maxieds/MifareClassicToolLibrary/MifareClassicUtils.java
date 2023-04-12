/*
This program (MifareClassicToolLibrary) is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

The complete license provided with source distributions of this library is
available at the following link:
https://github.com/maxieds/MifareClassicToolLibrary

Copyright by Maxie Schmidt and Gerhard Klostermeier.
*/

package com.maxieds.MifareClassicToolLibrary;

import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.GenericMFCException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.NFCErrorException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.NoKeysFoundException;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MifareClassicUtils {

     private static final String TAG = MifareClassicUtils.class.getSimpleName();

     public static final int MFCLASSIC1K_TAG_SIZE = 1024;
     public static final int MFCLASSIC1K_SECTOR_COUNT = 16;
     public static final int MFCLASSIC1K_BLOCKS_PER_SECTOR = 4;
     public static final int MFCLASSIC_BLOCK_SIZE = 16;

     public static String[] ExtractMFC1TagKeysFromDumpImage(InputStream dumpImageStream) {
          if(dumpImageStream == null) {
               return null;
          }
          String[] mfcTagKeyData = new String[2 * MFCLASSIC1K_SECTOR_COUNT];
          byte[] blockBytesBuf = new byte[MFCLASSIC_BLOCK_SIZE];
          byte[] keyBytes = new byte[6];
          int blockLineCount = 0, blockInSectorIndex = 0;
          try {
               for (int sec = 0; sec < MFCLASSIC1K_SECTOR_COUNT; sec++) {
                    for(int blk = 1; blk <= MFCLASSIC1K_BLOCKS_PER_SECTOR; blk++) {
                         if ((blockLineCount = dumpImageStream.read(blockBytesBuf, 0, MFCLASSIC_BLOCK_SIZE)) == -1) {
                              break;
                         } else if (blk == MFCLASSIC1K_BLOCKS_PER_SECTOR) { // trailing sector:
                              System.arraycopy(blockBytesBuf, 0, keyBytes, 0, 6);
                              mfcTagKeyData[2 * sec] = MCTUtils.BytesToHexString(keyBytes);
                              System.arraycopy(blockBytesBuf, 10, keyBytes, 0, 6);
                              mfcTagKeyData[2 * sec + 1] = MCTUtils.BytesToHexString(keyBytes);
                         }
                    }
               }
               dumpImageStream.close();
          } catch(IOException ioe) {
               ioe.printStackTrace();
               try {
                    dumpImageStream.close();
               } catch(IOException closeioe) {
                    closeioe.printStackTrace();
               }
               return null;
          }
          return mfcTagKeyData;
     }

     public static boolean WriteBlankMFC1KTag(android.nfc.Tag nfcTag, int rawResID, String[] keyDataList) throws MifareClassicLibraryException {
          // get a handle on the Mifare Classic tag:
          boolean writeTagStatus = true;
          android.nfc.tech.MifareClassic mfcTag = android.nfc.tech.MifareClassic.get(nfcTag);
          if(mfcTag == null) {
               throw new MifareClassicLibraryException(NFCErrorException);
          }
          try {
               mfcTag.connect();
               if(!mfcTag.isConnected()) {
                    throw new MifareClassicLibraryException(NFCErrorException);
               }
               Log.e(TAG, "NFC tag timeout: " + mfcTag.getTimeout());
               mfcTag.setTimeout(1500);
          } catch(IOException ioe) {
               ioe.printStackTrace();
               throw new MifareClassicLibraryException(NFCErrorException, ioe.getMessage());
          }
          // open the /res/raw file resource for the dump file we are going to be writing:
          android.content.Context appContext = MifareClassicToolLibrary.GetApplicationContext();
          try {
               InputStream rawFileStream = appContext.getResources().openRawResource(rawResID);
               byte[] dumpFileReadBuf = new byte[MFCLASSIC_BLOCK_SIZE];
               int bufReadCount, sectorAddr = -1, sectorBlockOffset = 0, activeKeyIndex = 0;
               int sectorBlockCount = mfcTag.getBlockCountInSector(0);
               int curSectorBlock = sectorBlockCount;
               int totalTagSectors = mfcTag.getSectorCount();
               while ((bufReadCount = rawFileStream.read(dumpFileReadBuf, 0, MFCLASSIC_BLOCK_SIZE)) != -1) {
                    if (bufReadCount < MFCLASSIC_BLOCK_SIZE) {
                         throw new MifareClassicLibraryException(GenericMFCException, "Unable to read entire block.");
                    }
                    if(curSectorBlock == sectorBlockCount) {
                         curSectorBlock = 0;
                         sectorAddr++;
                         sectorBlockOffset = mfcTag.sectorToBlock(sectorAddr);
                         sectorBlockCount = mfcTag.getBlockCountInSector(sectorAddr);
                         boolean ableToAuthKeyAB = false;
                         for(int r = 0; r < MifareClassicToolLibrary.RETRIES_TO_AUTH_KEYAB; r++) {
                              for (int k = 0; k < keyDataList.length; k++) {
                                   byte[] keyBytes = MCTUtils.HexStringToBytes(keyDataList[k]);
                                   if (mfcTag.authenticateSectorWithKeyA(sectorAddr, keyBytes)) {
                                        activeKeyIndex = k;
                                        ableToAuthKeyAB = true;
                                        break;
                                   } else if (mfcTag.authenticateSectorWithKeyB(sectorAddr, keyBytes)) {
                                        activeKeyIndex = k;
                                        ableToAuthKeyAB = true;
                                        break;
                                   }
                              }
                         }
                         if(!ableToAuthKeyAB) {
                              Log.e(TAG, "Could not auth with keyA/B on sector #" + sectorAddr);
                              writeTagStatus = false;
                              rawFileStream.close();
                              throw new MifareClassicLibraryException(NoKeysFoundException, "Could not auth with keyA/B on sector #" + sectorAddr);
                         }
                         Log.d(TAG, "Successfully authed with tag on sector #" + sectorAddr + " with key " + keyDataList[activeKeyIndex]);
                    }
                    if(sectorAddr > 0 || curSectorBlock > 0) {
                         mfcTag.writeBlock(sectorBlockOffset + curSectorBlock, dumpFileReadBuf);
                    }
                    curSectorBlock++;
                    MifareClassicToolLibrary.DisplayProgressBar("SECTOR WRITE", sectorAddr, totalTagSectors);
               }
               rawFileStream.close();
               mfcTag.close();
          } catch(IOException ioe) {
               ioe.printStackTrace();
               throw new MifareClassicLibraryException(NFCErrorException, ioe.getMessage());
          }
          return writeTagStatus;
     }

     public static String[] GetDumpImageContents(int dumpImageRawId) {
          InputStream dumpImageStream = MifareClassicToolLibrary.GetApplicationContext().getResources().openRawResource(dumpImageRawId);
          String[] dumpImageBlockData = new String[MFCLASSIC1K_SECTOR_COUNT * MFCLASSIC1K_BLOCKS_PER_SECTOR];
          byte[] blockBytes = new byte[MFCLASSIC_BLOCK_SIZE];
          int blockLineCount = 0, blkIndex = 0;
          try {
               while ((blockLineCount = dumpImageStream.read(blockBytes, 0, MFCLASSIC_BLOCK_SIZE)) != -1) {
                    if(blkIndex >= MFCLASSIC1K_SECTOR_COUNT * MFCLASSIC1K_BLOCKS_PER_SECTOR) {
                         Log.e(TAG, "Attempting to read more than a MFC1K #" + MFCLASSIC1K_SECTOR_COUNT * MFCLASSIC1K_BLOCKS_PER_SECTOR + " blocks...");
                         break;
                    }
                    else if(blockLineCount < MFCLASSIC_BLOCK_SIZE) {
                         Log.e(TAG, String.format(Locale.US, "Block #%d: only read %d of %d expected bytes ...", blkIndex, blockLineCount, MFCLASSIC_BLOCK_SIZE));
                         dumpImageStream.close();
                         return null;
                    }
                    dumpImageBlockData[blkIndex] = MCTUtils.BytesToHexString(blockBytes);
                    blkIndex++;
               }
               dumpImageStream.close();
          } catch(IOException ioe) {
               ioe.printStackTrace();
               return null;
          }
          return dumpImageBlockData;
     }

}

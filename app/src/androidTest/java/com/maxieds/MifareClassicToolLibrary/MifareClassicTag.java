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
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.InvalidKeysException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.NFCErrorException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.NoKeysFoundException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.NoTagException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.PartialReadException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.TagIOException;
import static com.maxieds.MifareClassicToolLibrary.MifareClassicLibraryException.MFCLibraryExceptionType.UnsupportedTagException;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;
import android.util.SparseArray;

import com.maxieds.MifareClassicToolLibrary.MCTUtils.DiffTimeTimer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import de.androidcrypto.mifareclassictoollibrarydemosdk33.R;

public class MifareClassicTag {

     private static final String TAG = MifareClassicTag.class.getSimpleName();

     public static final String NO_KEY = "------------";
     public static final String NO_DATA = "--------------------------------";

     public static class MFCSector {

          public int sectorAddress;
          public int sectorSize;
          public int sectorBlockCount;
          public int sectorFirstBlock;
          public int sectorBytesPerBlock;
          public String[] sectorBlockData;
          private MifareClassic m_mfcTag;
          public long timeToRead;

          public MFCSector() {
               sectorAddress = sectorSize = sectorBlockCount = sectorFirstBlock = sectorBytesPerBlock = 0;
               sectorBlockData = new String[0];
               m_mfcTag = null;
               timeToRead = 0;
          }

          public void FromTag(MifareClassic mfcTag, int saddr) throws MifareClassicLibraryException {
               if(mfcTag == null) {
                    throw new MifareClassicLibraryException(UnsupportedTagException);
               }
               sectorAddress = saddr;
               sectorFirstBlock = mfcTag.sectorToBlock(sectorAddress);
               sectorBlockCount = mfcTag.getBlockCountInSector(sectorAddress);
               sectorBytesPerBlock = MifareClassic.BLOCK_SIZE;
               sectorSize = sectorBlockCount * sectorBytesPerBlock;
               m_mfcTag = mfcTag;
          }

          public void AsMifareClassic1KTag(MifareClassic mfcTag, int saddr) {
               sectorAddress = saddr;
               sectorFirstBlock = sectorAddress * MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR;
               sectorBlockCount = MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR;
               sectorBytesPerBlock = MifareClassicUtils.MFCLASSIC_BLOCK_SIZE;
               sectorSize = sectorBlockCount * sectorBytesPerBlock;
               m_mfcTag = mfcTag;
          }

          public String[] ReadSector(int sectorIndex, byte[] key, boolean useAsKeyB) throws MifareClassicLibraryException {
               boolean auth = Authenticate(sectorIndex, key, useAsKeyB);
               String[] ret = null;
               // Read sector.
               if (auth) {
                    // Read all blocks.
                    ArrayList<String> blocks = new ArrayList<>();
                    int firstBlock = m_mfcTag.sectorToBlock(sectorIndex);
                    int lastBlock = firstBlock + 4;
                    if (m_mfcTag.getSize() == MifareClassic.SIZE_4K && sectorIndex > 31) {
                         lastBlock = firstBlock + 16;
                    }
                    for (int i = firstBlock; i < lastBlock; i++) {
                         try {
                              byte blockBytes[] = m_mfcTag.readBlock(i);
                              // mMFC.readBlock(i) must return 16 bytes or throw an error.
                              // At least this is what the documentation says.
                              // On Samsung's Galaxy S5 and Sony's Xperia Z2 however, it
                              // sometimes returns < 16 bytes for unknown reasons.
                              // Update: Aaand sometimes it returns more than 16 bytes...
                              // The appended byte(s) are 0x00.
                              if (blockBytes.length < 16) {
                                   throw new IOException();
                              }
                              if (blockBytes.length > 16) {
                                   blockBytes = Arrays.copyOf(blockBytes,16);
                              }

                              blocks.add(MCTUtils.BytesToHexString(blockBytes));
                         } catch (TagLostException e) {
                              throw new MifareClassicLibraryException(PartialReadException, e.getMessage());
                         } catch (IOException e) {
                              // Could not read block.
                              // (Maybe due to key/authentication method.)
                              Log.d(TAG, "(Recoverable) Error while reading block " + i + " from tag.");
                              blocks.add(NO_DATA);
                              if (!m_mfcTag.isConnected()) {
                                   throw new MifareClassicLibraryException(PartialReadException, "Tag removed during ReadSectorHelper.");
                              }
                              // After an error, a re-authentication is needed.
                              Authenticate(sectorIndex, key, useAsKeyB);
                         }
                    }
                    ret = blocks.toArray(new String[blocks.size()]);
                    int last = ret.length -1;
                    // Validate if it was possible to read any data.
                    boolean noData = true;
                    for (int i = 0; i < ret.length; i++) {
                         if (!ret[i].equals(NO_DATA)) {
                              noData = false;
                              break;
                         }
                    }
                    if (noData) {
                         // Was is possible to read any data (especially with key B)?
                         // If Key B may be read in the corresponding Sector Trailer,
                         // it cannot serve for authentication (according to NXP).
                         // What they mean is that you can authenticate successfully,
                         // but can not read data. In this case the
                         // readBlock() result is 0 for each block.
                         // Also, a tag might be bricked in a way that the authentication
                         // works, but reading data does not.
                         ret = null;
                    } else {
                         // Merge key in last block (sector trailer).
                         if (!useAsKeyB) {
                              if (IsKeyBReadable(MCTUtils.HexStringToBytes(ret[last].substring(12, 20)))) {
                                   ret[last] = MCTUtils.BytesToHexString(key) + ret[last].substring(12, 32);
                              }
                              else {
                                   ret[last] = MCTUtils.BytesToHexString(key) + ret[last].substring(12, 20) + NO_KEY;
                              }
                         }
                         else {
                              ret[last] = NO_KEY + ret[last].substring(12, 20) + MCTUtils.BytesToHexString(key);
                         }
                    }
               }
               return ret;
          }

          private boolean Authenticate(int sectorIndex, byte[] key, boolean useAsKeyB) {
               // Fetch the retry authentication option. Some tags and
               // devices have strange issues and need a retry in order to work...
               // Info: https://github.com/ikarus23/MifareClassicTool/issues/134
               // and https://github.com/ikarus23/MifareClassicTool/issues/106
               boolean retryAuth = MifareClassicToolLibrary.RETRIES_TO_AUTH_KEYAB > 0;
               int retryCount = MifareClassicToolLibrary.RETRIES_TO_AUTH_KEYAB;
               boolean ret = false;
               for (int i = 0; i < retryCount+1; i++) {
                    try {
                         if (!useAsKeyB) {
                              // Key A.
                              ret = m_mfcTag.authenticateSectorWithKeyA(sectorIndex, key);
                         } else {
                              // Key B.
                              ret = m_mfcTag.authenticateSectorWithKeyB(sectorIndex, key);
                         }
                    } catch (IOException e) {
                         Log.d(TAG, "Error authenticating with tag.");
                         return false;
                    }
                    if (ret || !retryAuth) { // Retry?
                         break;
                    }
               }
               return ret;
          }

          private boolean IsKeyBReadable(byte[] ac) {
               byte c1 = (byte) ((ac[1] & 0x80) >>> 7);
               byte c2 = (byte) ((ac[2] & 0x08) >>> 3);
               byte c3 = (byte) ((ac[2] & 0x80) >>> 7);
               return c1 == 0
                    && (c2 == 0 && c3 == 0)
                    || (c2 == 1 && c3 == 0)
                    || (c2 == 0 && c3 == 1);
          }

          private String[] MergeSectorData(String[] firstResult, String[] secondResult) {
               String[] ret = null;
               if (firstResult != null || secondResult != null) {
                    if ((firstResult != null && secondResult != null)
                         && firstResult.length != secondResult.length) {
                         return null;
                    }
                    int length  = (firstResult != null) ? firstResult.length : secondResult.length;
                    ArrayList<String> blocks = new ArrayList<>();
                    // Merge data blocks.
                    for (int i = 0; i < length -1 ; i++) {
                         if (firstResult != null && firstResult[i] != null
                              && !firstResult[i].equals(NO_DATA)) {
                              blocks.add(firstResult[i]);
                         }
                         else if (secondResult != null && secondResult[i] != null
                              && !secondResult[i].equals(NO_DATA)) {
                              blocks.add(secondResult[i]);
                         }
                         else {
                              // None of the results got the data form the block.
                              blocks.add(NO_DATA);
                         }
                    }
                    ret = blocks.toArray(new String[blocks.size() + 1]);
                    int last = length - 1;
                    // Merge sector trailer.
                    if (firstResult != null && firstResult[last] != null
                         && !firstResult[last].equals(NO_DATA)) {
                         // Take first for sector trailer.
                         ret[last] = firstResult[last];
                         if (secondResult != null && secondResult[last] != null
                              && !secondResult[last].equals(NO_DATA)) {
                              // Merge key form second result to sector trailer.
                              ret[last] = ret[last].substring(0, 20) + secondResult[last].substring(20);
                              Log.d(TAG, "Correctly merging sector trailer ...");
                         }
                    }
                    else if (secondResult != null && secondResult[last] != null
                         && !secondResult[last].equals(NO_DATA)) {
                         // No first result. Take second result as sector trailer.
                         ret[last] = secondResult[last];
                    }
                    else {
                         // No sector trailer at all.
                         ret[last] = NO_DATA;
                    }
               }
               return ret;
          }

          public boolean ContainsInvalidBlockData() {
               if(sectorBlockData == null) {
                    return false;
               }
               for(int blk = 0; blk < sectorBlockData.length; blk++) {
                    if(sectorBlockData[blk] != null && sectorBlockData[blk].contains("-")) {
                         return true;
                    }
                    else if(sectorBlockData[blk] == null) {
                         return false;
                    }
               }
               return false;
          }

     }

     /**
      * Patch a possibly broken Tag object of HTC One (m7/m8) or Sony
      * Xperia Z3 devices (with Android 5.x.)
      *
      * HTC One: "It seems, the reason of this bug is TechExtras of NfcA is null.
      * However, TechList contains MifareClassic." -- bildin.
      * This method will fix this. For more information please refer to
      * https://github.com/ikarus23/MifareClassicTool/issues/52
      * This patch was provided by bildin (https://github.com/bildin).
      *
      * Sony Xperia Z3 (+ emmulated MIFARE Classic tag): The buggy tag has
      * two NfcA in the TechList with different SAK values and a MifareClassic
      * (with the Extra of the second NfcA). Both, the second NfcA and the
      * MifareClassic technique, have a SAK of 0x20. According to NXP's
      * guidelines on identifying MIFARE tags (Page 11), this a MIFARE Plus or
      * MIFARE DESFire tag. This method creates a new Extra with the SAK
      * values of both NfcA occurrences ORed (as mentioned in NXP's
      * MIFARE type identification procedure guide) and replace the Extra of
      * the first NfcA with the new one. For more information please refer to
      * https://github.com/ikarus23/MifareClassicTool/issues/64
      * This patch was provided by bildin (https://github.com/bildin).
      *
      * @param tag The possibly broken tag.
      * @return The fixed tag.
      */
     public static Tag patchTag(Tag tag) {
          if (tag == null) {
               return null;
          }
          String[] techList = tag.getTechList();
          Parcel oldParcel = Parcel.obtain();
          tag.writeToParcel(oldParcel, 0);
          oldParcel.setDataPosition(0);
          int len = oldParcel.readInt();
          byte[] id = new byte[0];
          if (len >= 0) {
               id = new byte[len];
               oldParcel.readByteArray(id);
          }
          int[] oldTechList = new int[oldParcel.readInt()];
          oldParcel.readIntArray(oldTechList);
          Bundle[] oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR);
          int serviceHandle = oldParcel.readInt();
          int isMock = oldParcel.readInt();
          IBinder tagService;
          if (isMock == 0) {
               tagService = oldParcel.readStrongBinder();
          } else {
               tagService = null;
          }
          oldParcel.recycle();
          int nfcaIdx = -1;
          int mcIdx = -1;
          short sak = 0;
          boolean isFirstSak = true;
          for (int i = 0; i < techList.length; i++) {
               if (techList[i].equals(NfcA.class.getName())) {
                    if (nfcaIdx == -1) {
                         nfcaIdx = i;
                    }
                    if (oldTechExtras[i] != null && oldTechExtras[i].containsKey("sak")) {
                         sak = (short) (sak | oldTechExtras[i].getShort("sak"));
                         isFirstSak = nfcaIdx == i;
                    }
               } else if (techList[i].equals(MifareClassic.class.getName())) {
                    mcIdx = i;
               }
          }
          boolean modified = false;
          // Patch the double NfcA issue (with different SAK) for
          // Sony Z3 devices.
          if (!isFirstSak) {
               oldTechExtras[nfcaIdx].putShort("sak", sak);
               modified = true;
          }
          // Patch the wrong index issue for HTC One devices.
          if (nfcaIdx != -1 && mcIdx != -1 && oldTechExtras[mcIdx] == null) {
               oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx];
               modified = true;
          }
          if (!modified) {
               // Old tag was not modivied. Return the old one.
               return tag;
          }
          // Old tag was modified. Create a new tag with the new data.
          Parcel newParcel = Parcel.obtain();
          newParcel.writeInt(id.length);
          newParcel.writeByteArray(id);
          newParcel.writeInt(oldTechList.length);
          newParcel.writeIntArray(oldTechList);
          newParcel.writeTypedArray(oldTechExtras, 0);
          newParcel.writeInt(serviceHandle);
          newParcel.writeInt(isMock);
          if (isMock == 0) {
               newParcel.writeStrongBinder(tagService);
          }
          newParcel.setDataPosition(0);
          Tag newTag = Tag.CREATOR.createFromParcel(newParcel);
          newParcel.recycle();
          return newTag;
     }

     private String mfcTagType;
     private int tagSize, tagSectorCount, tagBlockCount, tagBytesPerBlock;
     private String[] mfcDumpImageData;
     private List<MFCSector> failedSectors, tagSectors;
     private String rfTechCaps, tagManufacturer;
     private String tagUID, tagATQA, tagSAK, tagATS;
     private long totalTimeToRead;
     private boolean displayGUIProgressBar;

     // MCT accounting variables:
     private MifareClassic m_mfcTag;
     private SparseArray<byte[][]> mKeyMap;
     private int mKeyMapStatus;
     private int mLastSector;
     private int mFirstSector;
     private ArrayList<byte[]> mKeysWithOrder;

     private boolean ResetParameters() {
          mfcTagType = "";
          tagSize = tagSectorCount = tagBlockCount = tagBytesPerBlock = 0;
          mfcDumpImageData = null;
          failedSectors = new ArrayList<MFCSector>();
          tagSectors = new ArrayList<MFCSector>();
          rfTechCaps = tagManufacturer = "";
          tagUID = tagATQA = tagSAK = tagATS = "";
          totalTimeToRead = 0;
          displayGUIProgressBar = true;
          m_mfcTag = null;
          mKeyMap = new SparseArray<>();
          mKeyMapStatus = 0;
          mLastSector = -1;
          mFirstSector = 0;
          mKeysWithOrder = new ArrayList();
          return true;
     }

     private MifareClassicTag() {
          ResetParameters();
     }

     public static final int MFC_FULL_SUPPORT = 0;
     public static final int NO_MFC_DEVICE_SUPPORT = -1;
     public static final int NO_MFC_TAG_SUPPORT = -2;
     public static final int MFC_TAG_ERROR = -3;

     public static int CheckMifareClassicSupport(Tag nfcTag) {
          if(nfcTag == null) {
               return MFC_TAG_ERROR;
          }
          else if(Arrays.asList(nfcTag.getTechList()).contains(MifareClassic.class.getName())) {
               // Device and tag support MIFARE Classic.
               return MFC_FULL_SUPPORT;
          }
          else {
               // Check if device does not support MIFARE Classic.
               // For doing so, check if the SAK of the tag indicate that
               // it's a MIFARE Classic tag.
               // See: https://www.nxp.com/docs/en/application-note/AN10834.pdf
               NfcA nfca = NfcA.get(nfcTag);
               byte sak = (byte) nfca.getSak();
               if ((sak >> 1 & 1) == 1) { // RFU.
                    return NO_MFC_TAG_SUPPORT;
               } else {
                    if ((sak >> 3 & 1) == 1) { // SAK bit 4 = 1?
                         if ((sak >> 4 & 1) == 1) { // SAK bit 5 = 1?
                              // MIFARE Classic 4k
                              // MIFARE SmartMX 4K
                              // MIFARE PlusS 4K SL1
                              // MIFARE PlusX 4K SL1
                              return NO_MFC_DEVICE_SUPPORT;
                         } else {
                              if ((sak & 1) == 1) { // SAK bit 1 = 1?
                                   // MIFARE Mini
                                   return NO_MFC_DEVICE_SUPPORT;
                              } else {
                                   // MIFARE Classic 1k
                                   // MIFARE SmartMX 1k
                                   // MIFARE PlusS 2K SL1
                                   // MIFARE PlusX 2K SL2
                                   return NO_MFC_DEVICE_SUPPORT;
                              }
                         }
                    } else {
                         // Some MIFARE tag, but not Classic or Classic compatible.
                         return NO_MFC_TAG_SUPPORT;
                    }
               }
          }
     }

     public static MifareClassicTag Decode(Tag nfcTag, String[] keyData, boolean displayGUIProgressBar) throws MifareClassicLibraryException {
          if(nfcTag == null) {
               throw new MifareClassicLibraryException(NoTagException);
          }
          else if(keyData == null) {
               throw new MifareClassicLibraryException(NoKeysFoundException);
          }
          else if(!CheckMFCKeys(keyData)) {
               throw new MifareClassicLibraryException(InvalidKeysException);
          }
          else if(!MifareClassicToolLibrary.Initialized()) {
               throw new MifareClassicLibraryException(GenericMFCException, "Uninitialized NFC tag data");
          }
          nfcTag = patchTag(nfcTag);
          int mfcSupportCode = 0;
          if((mfcSupportCode = CheckMifareClassicSupport(nfcTag)) != 0) {
               throw new MifareClassicLibraryException(UnsupportedTagException);
          }
          if(nfcTag == null) {
               throw new MifareClassicLibraryException(NFCErrorException);
          }
          else if(keyData == null || keyData.length == 0) {
               throw new MifareClassicLibraryException(InvalidKeysException);
          }
          MifareClassic mfcTag = MifareClassic.get(nfcTag);
          if(mfcTag == null) {
               throw new MifareClassicLibraryException(NFCErrorException);
          }
          try {
               mfcTag.setTimeout(500);
               mfcTag.connect();
               if(!mfcTag.isConnected()) {
                    throw new MifareClassicLibraryException(NFCErrorException);
               }
          } catch(IOException ioe) {
               ioe.printStackTrace();
               throw new MifareClassicLibraryException(NFCErrorException, ioe.getMessage());
          }
          MifareClassicTag mfcTagData = new MifareClassicTag();
          mfcTagData.m_mfcTag = mfcTag;
          mfcTagData.mKeysWithOrder.addAll(KeyStringsToByteData(keyData));
          mfcTagData.displayGUIProgressBar = displayGUIProgressBar;
          if(!mfcTagData.ReadTagReservedData(nfcTag) ||
               !mfcTagData.IdentifyTag(mfcSupportCode) ||
               !mfcTagData.DumpTag()) {
               return null;
          }
          return mfcTagData;
     }

     public static MifareClassicTag Decode(Tag nfcTag, boolean displayGUIProgressBar) throws MifareClassicLibraryException {
          String[] defaultKeyData = new String[] {
               //"000000000000",
               MCTUtils.BytesToHexString(MifareClassic.KEY_DEFAULT),
               MCTUtils.BytesToHexString(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY),
               MCTUtils.BytesToHexString(MifareClassic.KEY_NFC_FORUM)
          };
          return MifareClassicTag.Decode(nfcTag, defaultKeyData, displayGUIProgressBar);
     }

     private boolean ReadTagReservedData(Tag nfcTag) throws MifareClassicLibraryException {
          tagUID = MCTUtils.BytesToHexString(nfcTag.getId());
          NfcA nfcaTag = NfcA.get(nfcTag);
          // Swap ATQA to match the common order like shown here:
          // http://nfc-tools.org/index.php?title=ISO14443A
          byte[] atqaBytes = nfcaTag.getAtqa();
          atqaBytes = new byte[] {atqaBytes[1], atqaBytes[0]};
          tagATQA = MCTUtils.BytesToHexString(atqaBytes);
          // SAK in big endian.
          byte[] sakBytes = new byte[] {
               (byte) ((nfcaTag.getSak() >> 8) & 0xFF),
               (byte) (nfcaTag.getSak() & 0xFF)
          };
          // Print the first SAK byte only if it is not 0.
          if (sakBytes[0] != 0x00) {
               tagSAK = MCTUtils.BytesToHexString(sakBytes);
          }
          else {
               tagSAK = MCTUtils.BytesToHexString(new byte[] { sakBytes[1] });
          }
          tagATS = "-";
          IsoDep isoTag = IsoDep.get(nfcTag);
          if (isoTag != null) {
               byte[] atsBytes = isoTag.getHistoricalBytes();
               if (atsBytes != null && atsBytes.length > 0) {
                    tagATS = MCTUtils.BytesToHexString(atsBytes);
               }
          }
          return true;
     }

     private boolean IdentifyTag(int mfcSupportCode) throws MifareClassicLibraryException {
          // Identify tag type (by string).
          boolean successCode = true;
          int tagTypeResourceID = GetTagIdentifier(tagATQA, tagSAK, tagATS);
          if (tagTypeResourceID == R.string.tag_unknown && mfcSupportCode > NO_MFC_TAG_SUPPORT) {
               mfcTagType = MifareClassicToolLibrary.GetApplicationContext().getString(R.string.tag_unknown_mf_classic);
               successCode = false;
          }
          else {
               mfcTagType = MifareClassicToolLibrary.GetApplicationContext().getString(tagTypeResourceID);
          }
          // read in the logistical layout data (e.g., number of sectors, tag size, etc.):
          if(mfcSupportCode != 0) {
               successCode = false;
          }
          else if(m_mfcTag == null) {
               throw new MifareClassicLibraryException(NFCErrorException, "MifareClassic tag is null");
          }
          else {
               tagSize = m_mfcTag.getSize();
               tagSectorCount = m_mfcTag.getSectorCount();
               tagBlockCount = m_mfcTag.getBlockCount();
               tagBytesPerBlock = MifareClassic.BLOCK_SIZE;
          }
          return successCode;
     }

     private boolean DumpTag() throws MifareClassicLibraryException {
          if(m_mfcTag == null) {
               throw new MifareClassicLibraryException(NFCErrorException, "MifareClassic tag is null");
          }
          DiffTimeTimer tagReadTimer = new DiffTimeTimer();
          tagReadTimer.startTimer();
          for(int sec = 0; sec < GetSectorCount(); sec++) {
               MFCSector nextSector = new MFCSector();
               nextSector.AsMifareClassic1KTag(m_mfcTag, sec);
               nextSector.sectorBlockData = new String[] { NO_DATA, NO_DATA, NO_DATA, NO_DATA };
               nextSector.timeToRead = 0;
               tagSectors.add(nextSector);
          }
          SetMappingRange(0, GetSectorCount() - 1);
          SparseArray<String[]> sectorReadData = ReadAsMuchAsPossible();
          if(sectorReadData == null) {
               failedSectors.addAll(tagSectors);
               return true;
          }
          for(int kidx = 0; kidx < sectorReadData.size(); kidx++) {
               int sectorAddr = sectorReadData.keyAt(kidx);
               String[] sectorBlockData = sectorReadData.valueAt(kidx);
               tagSectors.get(sectorAddr).sectorBlockData = sectorBlockData;
               if(tagSectors.get(sectorAddr).ContainsInvalidBlockData()) {
                    failedSectors.add(tagSectors.get(sectorAddr));
               }
          }
          totalTimeToRead = tagReadTimer.diffTimer();
          try {
               m_mfcTag.close();
          } catch(IOException ioe) {
               ioe.printStackTrace();
               throw new MifareClassicLibraryException(NFCErrorException, ioe.getMessage());
          }
          return true;
     }

     private int GetTagIdentifier(String atqa, String sak, String ats) {
          String prefix = "tag_";
          ats = ats.replace("-", "");
          // First check on ATQA + SAK + ATS.
          int ret = MifareClassicToolLibrary.GetApplicationContext().getResources().getIdentifier(
               prefix + atqa + sak + ats, "string", MifareClassicToolLibrary.GetApplicationContext().getPackageName());
          if (ret == 0) {
               // Check on ATQA + SAK.
               ret = MifareClassicToolLibrary.GetApplicationContext().getResources().getIdentifier(
                    prefix + atqa + sak, "string", MifareClassicToolLibrary.GetApplicationContext().getPackageName());
          }
          if (ret == 0) {
               // Check on ATQA.
               ret = MifareClassicToolLibrary.GetApplicationContext().getResources().getIdentifier(
                    prefix + atqa, "string", MifareClassicToolLibrary.GetApplicationContext().getPackageName());
          }
          if (ret == 0) {
               // No match found return "Unknown".
               return R.string.tag_unknown;
          }
          return ret;
     }

     public static boolean CheckMFCKeys(String[] keyDataList) {
          if(keyDataList == null) {
               return false;
          }
          for(int kidx = 0; kidx < keyDataList.length; kidx++) {
               if(!MCTUtils.IsHexAnd6Byte(keyDataList[kidx])) {
                    return false;
               }
          }
          return true;
     }

     public static ArrayList<byte[]> KeyStringsToByteData(String[] keysList) {
          ArrayList<byte[]> keyByteDataList = new ArrayList();
          for(int k = 0; k < keysList.length; k++) {
               keyByteDataList.add(MCTUtils.HexStringToBytes(keysList[k]));
          }
          return keyByteDataList;
     }

     /**
      * Read as much as possible from the tag with the given key information.
      * @param keyMap Keys (A and B) mapped to a sector.
      * See {@link #BuildNextKeyMapPart()}.
      * @return A Key-Value Pair. Keys are the sector numbers, values
      * are the tag data. This tag data (values) are arrays containing
      * one block per field (index 0-3 or 0-15).
      * If a block is "null" it means that the block couldn't be
      * read with the given key information.<br />
      * On Error, "null" will be returned (tag was removed during reading or
      * keyMap is null). If none of the keys in the key map are valid for reading
      * (and therefore no sector is read), an empty set (SparseArray.size() == 0)
      * will be returned.
      * @see #BuildNextKeyMapPart()
      */
     private SparseArray<String[]> ReadAsMuchAsPossible(SparseArray<byte[][]> keyMap) throws MifareClassicLibraryException {
          SparseArray<String[]> resultSparseArray;
          //DiffTimeTimer sectorReadTimer = new DiffTimeTimer();
          if (keyMap != null && keyMap.size() > 0) {
               resultSparseArray = new SparseArray<>(keyMap.size());
               // For all entries in map do:
               for (int i = 0; i < keyMap.size(); i++) {
                    //if(displayGUIProgressBar) {
                    //     MifareClassicToolLibrary.DisplayProgressBar("SECTOR", i, keyMap.size());
                    //}
                    MifareClassicToolLibrary.GetDataInterface().PostTagScanSectorReadProgress(i, keyMap.size());
                    String[][] results = new String[2][];
                    if (keyMap.valueAt(i)[0] != null) {
                         // Read with key A.
                         //sectorReadTimer.startTimer();
                         int sectorAddr = keyMap.keyAt(i);
                         //Log.d(TAG, "Sector #" + sectorAddr + " keyA = " + MCTUtils.BytesToHexString(keyMap.valueAt(i)[0]));
                         results[0] = tagSectors.get(sectorAddr).ReadSector(sectorAddr, keyMap.valueAt(i)[0], false);
                         //tagSectors.get(sectorAddr).timeToRead += sectorReadTimer.diffTimer();
                    }
                    if (keyMap.valueAt(i)[1] != null) {
                         // Read with key B.
                         //sectorReadTimer.startTimer();
                         int sectorAddr = keyMap.keyAt(i);
                         //Log.d(TAG, "Sector #" + sectorAddr + " keyB = " + MCTUtils.BytesToHexString(keyMap.valueAt(i)[1]));
                         results[1] = tagSectors.get(sectorAddr).ReadSector(sectorAddr, keyMap.valueAt(i)[1], true);
                         //tagSectors.get(sectorAddr).timeToRead += sectorReadTimer.diffTimer();
                    }
                    // Merge results.
                    if (results[0] != null || results[1] != null) {
                         //sectorReadTimer.startTimer();
                         int sectorAddr = keyMap.keyAt(i);
                         //Log.d(TAG, "Merging sector #" + sectorAddr);
                         resultSparseArray.put(sectorAddr, tagSectors.get(sectorAddr).MergeSectorData(results[0], results[1]));
                         //tagSectors.get(sectorAddr).timeToRead += sectorReadTimer.diffTimer();
                    }
               }
               return resultSparseArray;
          }
          return null;
     }

     /**
      * Read as much as possible from the tag depending on the
      * mapping range and the given key information.
      * The key information must be set before calling this method.
      * Also the mapping range must be specified before calling this method
      * (use {@link #SetMappingRange(int, int)}).
      * Attention: This method builds a key map. Depending on the key count
      * in the given key file, this could take more than a few minutes.
      * The old key map from {@link #GetKeyMap()} will be destroyed and
      * the full new one is gettable afterwards.
      * @return A Key-Value Pair. Keys are the sector numbers, values
      * are the tag data. The tag data (values) are arrays containing
      * one block per field (index 0-3 or 0-15).
      * If a block is "null" it means that the block couldn't be
      * read with the given key information.
      * @see #BuildNextKeyMapPart()
      */
     private SparseArray<String[]> ReadAsMuchAsPossible() throws MifareClassicLibraryException {
          mKeyMapStatus = GetSectorCount();
          while (BuildNextKeyMapPart() < GetSectorCount()-1);
          return ReadAsMuchAsPossible(mKeyMap);
     }

     /**
      * Build Key-Value Pairs in which keys represent the sector and
      * values are one or both of the MIFARE keys (A/B).
      * Also the mapping range must be specified before calling this method
      * (use {@link #SetMappingRange(int, int)}).<br /><br />
      * The mapping works like some kind of dictionary attack.
      * All keys are checked against the next sector
      * with both authentication methods (A/B). If at least one key was found
      * for a sector, the map will be extended with an entry, containing the
      * key(s) and the information for what sector the key(s) are. You can get
      * this Key-Value Pairs by calling {@link #GetKeyMap()}. A full
      * key map can be gained by calling this method as often as there are
      * sectors on the tag (See {@link #GetSectorCount()}). If you call
      * this method once more after a full key map was created, it resets the
      * key map and starts all over.
      * @return The sector that was just checked. On an error condition,
      * it returns "-1" and resets the key map to "null".
      * @see #GetKeyMap()
      * @see #SetMappingRange(int, int)
      * @see #ReadAsMuchAsPossible(SparseArray)
      */
     public int BuildNextKeyMapPart() throws MifareClassicLibraryException {
          // Clear status and key map before new walk through sectors.
          boolean error = false;
          String excptErrorMsg = "";
          if (mKeysWithOrder != null && mLastSector != -1) {
               if (mKeyMapStatus == mLastSector + 1) {
                    mKeyMapStatus = mFirstSector;
                    mKeyMap = new SparseArray<>();
               }
               // Get auto reconnect setting.
               boolean autoReconnect = MifareClassicToolLibrary.AUTORECONNECT;
               // Get retry authentication option.
               int retryAuthCount = MifareClassicToolLibrary.RETRIES_TO_AUTH_KEYAB;
               boolean retryAuth = retryAuthCount > 0;

               byte[][] keys = new byte[2][];
               boolean[] foundKeys = new boolean[] {false, false};
               boolean auth;

               // Check next sector against all keys (lines) with
               // authentication method A and B.
               Log.d(TAG, "Building ... Number of keys = " + mKeysWithOrder.size());
               keysloop:
               for (int i = 0; i < mKeysWithOrder.size(); i++) {
                    if(displayGUIProgressBar) {
                         MifareClassicToolLibrary.DisplayProgressBar("KEYS",
                                          mKeyMapStatus * mKeysWithOrder.size() + i,
                                         GetSectorCount() * mKeysWithOrder.size());
                    }
                    MifareClassicToolLibrary.GetDataInterface().PostTagScanKeyMapProgress(mKeyMapStatus * mKeysWithOrder.size() + i, GetSectorCount() * mKeysWithOrder.size());
                    byte[] key = mKeysWithOrder.get(i);
                    for (int j = 0; j < retryAuthCount+1;) {
                         try {
                              if (!foundKeys[0]) {
                                   auth = m_mfcTag.authenticateSectorWithKeyA(mKeyMapStatus, key);
                                   if (auth) {
                                        keys[0] = key;
                                        foundKeys[0] = true;
                                        Log.d(TAG, "Building " + mKeyMapStatus + " ... KeyA with " + MCTUtils.BytesToHexString(key));
                                   }
                              }
                              if (!foundKeys[1]) {
                                   auth = m_mfcTag.authenticateSectorWithKeyB(mKeyMapStatus, key);
                                   if (auth) {
                                        keys[1] = key;
                                        foundKeys[1] = true;
                                        Log.d(TAG, "Building " + mKeyMapStatus + " ... KeyB with " + MCTUtils.BytesToHexString(key));
                                   }
                              }
                         } catch (Exception e) {
                              Log.d(TAG, "Error while building next key map part");
                              // Is auto reconnect enabled?
                              if (autoReconnect) {
                                   Log.d(TAG, "Auto reconnect is enabled");
                                   while (!m_mfcTag.isConnected()) {
                                        // Sleep for 500ms.
                                        try {
                                             Thread.sleep(500);
                                        } catch (InterruptedException ex) {}
                                        // Try to reconnect.
                                        try {
                                             ConnectToMFCTag();
                                        } catch (Exception ex) {
                                             // Do nothing.
                                        }
                                   }
                                   // Repeat last loop (do not incr. j).
                                   continue;
                              } else {
                                   excptErrorMsg = e.getMessage();
                                   error = true;
                                   break keysloop;
                              }
                         }
                         // Retry?
                         if((foundKeys[0] && foundKeys[1]) || !retryAuth) {
                              // Both keys found or no retry wanted. Stop retrying.
                              break;
                         }
                         j++;
                    }
                    // Next key?
                    if ((foundKeys[0] && foundKeys[1])) {
                         // Both keys found. Stop searching for keys.
                         break;
                    }
               }
               if (!error && (foundKeys[0] || foundKeys[1])) {
                    // At least one key found. Add key(s).
                    mKeyMap.put(mKeyMapStatus, keys);
                    // Key reuse is very likely, so try the found keys second.
                    // NOTE: The all-F key has to be tested always first if there
                    // is a all-0 key in the key file, because of a bug in
                    // some tags and/or devices.
                    // https://github.com/ikarus23/MifareClassicTool/issues/66
                    if (foundKeys[0]) {
                         mKeysWithOrder.remove(keys[0]);
                         mKeysWithOrder.add(1, keys[0]);
                    }
                    if (foundKeys[1]) {
                         mKeysWithOrder.remove(keys[1]);
                         mKeysWithOrder.add(1, keys[1]);
                    }
               }
               mKeyMapStatus++;
          } else {
               error = true;
          }

          if (error) {
               mKeyMapStatus = 0;
               mKeyMap = null;
               throw new MifareClassicLibraryException(TagIOException, excptErrorMsg);
          }
          return mKeyMapStatus - 1;
     }

     /**
      * Set the mapping range for {@link #BuildNextKeyMapPart()}.
      * @param firstSector Index of the first sector of the key map.
      * @param lastSector Index of the last sector of the key map.
      * @return True if range parameters were correct. False otherwise.
      */
     private boolean SetMappingRange(int firstSector, int lastSector) {
          if (firstSector >= 0 && lastSector < GetSectorCount()
               && firstSector <= lastSector) {
               mFirstSector = firstSector;
               mLastSector = lastSector;
               // Initial status of buildNextKeyMapPart to create a new key map:
               mKeyMapStatus = lastSector + 1;
               return true;
          }
          return false;
     }

     /**
      * Connect the reader to the tag. If the reader is already connected the
      * "connect" will be skipped. If "connect" will block for more than 500ms
      * then connecting will be aborted.
      * @throws Exception Something went wrong while connecting to the tag.
      */
     private void ConnectToMFCTag() throws MifareClassicLibraryException {
          final AtomicBoolean error = new AtomicBoolean(false);
          // Do not connect if already connected.
          if (m_mfcTag.isConnected()) {
               return;
          }
          // Connect in a worker thread. (connect() might be blocking).
          Thread connectThread = new Thread(new Runnable() {
               @Override
               public void run() {
                    try {
                         m_mfcTag.connect();
                    } catch(Exception e) {
                         e.printStackTrace();
                         error.set(true);
                    }
               }
          });
          connectThread.start();
          // Wait for the connection (max 500 milliseconds).
          try {
               connectThread.join(500);
          } catch (InterruptedException ex) {
               ex.printStackTrace();
               error.set(true);
          }
          // If there was an error log it and throw an exception.
          if (error.get()) {
               Log.d(TAG, "Error while connecting to tag.");
               throw new MifareClassicLibraryException(NFCErrorException, "Error while connecting to tag.");
          }
     }

     private SparseArray<byte[][]> GetKeyMap() {
          return mKeyMap;
     }

     public String[] GetMFCDumpImageData() {
          return mfcDumpImageData;
     }

     public String GetTagType() {
          return mfcTagType;
     }

     public int GetSectorCount() {
          return tagSectorCount;
     }

     public int GetBlockCount() {
          return tagBlockCount;
     }

     public int GetBytesPerBlock() {
          return tagBytesPerBlock;
     }

     public int GetBlockCountInSector() { return MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR; }

     public int GetSize() {
          return tagSize;
     }

     public List<MFCSector> GetFailedSectors() {
          return failedSectors;
     }

     public String GetRFTechCaps() {
          return rfTechCaps;
     }

     public String GetManufacturer() {
          return tagManufacturer;
     }

     public String GetTagUID() {
          return tagUID;
     }

     public String GetTagUID(String byteSep) {
          return tagUID.substring(0, 2) + tagUID.substring(2).replaceAll("(.{2})", byteSep + "$1");
     }

     public int GetTagUIDSize() {
          return tagUID.length() / 2;
     }

     public String GetATQA() {
          return tagATQA;
     }

     public String GetSAK() {
          return tagSAK;
     }

     public String GetATS() {
          return tagATS;
     }

     public long GetTotalReadTime() { return totalTimeToRead; }

     public static String GetTagByteCountString(int byteCount) {
          if(byteCount == 1024) {
               return "1K";
          }
          else if(byteCount == 2048) {
               return "2K";
          }
          else if(byteCount == 4096) {
               return "4K";
          }
          else if(byteCount == 8192) {
               return "8K";
          }
          return String.valueOf(byteCount) + "B";
     }

     public String GetTagSizeSpecString() {
          String specString = String.format(Locale.US, "%s | %d Sectors x %d Blocks @ %sB",
               GetTagByteCountString(GetSize()), tagSectorCount,
               tagBlockCount / tagSectorCount, tagBytesPerBlock);
          return specString;
     }

     public boolean ExportToHexFile(String outputFile) throws IOException {
          if(outputFile == null || mfcDumpImageData == null) {
               return false;
          }
          PrintWriter printWriter = new PrintWriter(outputFile, "UTF-8");
          for(int blk = 0; blk < tagBlockCount; blk += tagBytesPerBlock) {
               String blkDataStr = mfcDumpImageData[blk].replace("-", "0");
               printWriter.print(blkDataStr);
          }
          printWriter.close();
          return true;
     }

     public boolean ExportToBinaryDumpFile(String outputFile) throws IOException {
          if(outputFile == null || mfcDumpImageData == null) {
               return false;
          }
          FileOutputStream outStream = new FileOutputStream(outputFile);
          for(int blk = 0; blk < mfcDumpImageData.length; blk++) {
               String blkDataStr = mfcDumpImageData[blk].replace("-", "0");
               outStream.write(MCTUtils.HexStringToBytes(blkDataStr));
          }
          outStream.close();
          return true;
     }

     public MFCSector GetSectorByIndex(int index) {
          if(index < 0 || index >= tagSectors.size()) {
               return null;
          }
          return tagSectors.get(index);
     }

     public boolean GetSectorReadStatus(int index) {
          if(index < 0) {
               return false;
          }
          for(int fsec = 0; fsec < failedSectors.size(); fsec++) {
               if(failedSectors.get(fsec).sectorAddress == index) {
                    return false;
               }
          }
          return true;
     }

     public static MifareClassicTag LoadMifareClassic1KFromResource(int resID) {

          if(!MifareClassicToolLibrary.Initialized()) {
               Log.e(TAG, "ERROR: MifareClassicToolLibrary NOT initialized!");
               return null;
          }

          // initialize statically "known" fields for a MFC1K tag:
          MifareClassicTag mfcTagData = new MifareClassicTag();
          mfcTagData.mfcTagType = "Mifare Classic 1K (From Dump Image)";
          mfcTagData.tagSize = MifareClassicUtils.MFCLASSIC1K_TAG_SIZE;
          mfcTagData.tagSectorCount = 16;
          mfcTagData.tagBytesPerBlock = MifareClassicUtils.MFCLASSIC_BLOCK_SIZE;
          mfcTagData.tagBlockCount = mfcTagData.tagSize / mfcTagData.tagBytesPerBlock;
          mfcTagData.tagManufacturer = "Unknown";

          // initialize the tag data bytes from the dump image resource:
          Context appMainContext = MifareClassicToolLibrary.GetApplicationContext();
          try {
               java.io.InputStream rawFileStream = appMainContext.getResources().openRawResource(resID);
               mfcTagData.mfcDumpImageData = new String[mfcTagData.tagBlockCount];
               int bytesReadCount = 0, blkIndex = 0;
               byte[] byteReadBuffer = new byte[MifareClassicUtils.MFCLASSIC_BLOCK_SIZE];
               while (bytesReadCount < MifareClassicUtils.MFCLASSIC1K_TAG_SIZE) {
                    int readByteCount = rawFileStream.read(byteReadBuffer, 0, MifareClassicUtils.MFCLASSIC_BLOCK_SIZE);
                    if (readByteCount < 0) {
                         break;
                    }
                    mfcTagData.mfcDumpImageData[blkIndex] = MCTUtils.BytesToHexString(byteReadBuffer);
                    bytesReadCount += readByteCount;
                    blkIndex++;
               }
               if(bytesReadCount < MifareClassicUtils.MFCLASSIC1K_TAG_SIZE) {
                    Log.e(TAG, "ERROR: Only able to load " + bytesReadCount + " of " + MifareClassicUtils.MFCLASSIC1K_TAG_SIZE + "bytes from tag!");
                    return null;
               }
          } catch(IOException ioe) {
               ioe.printStackTrace();
               return null;
          }
          // setup the individual sector data:
          for(int sec = 0; sec < mfcTagData.tagSectorCount; sec++) {
               MFCSector nextSector = new MFCSector();
               nextSector.sectorAddress = sec;
               nextSector.sectorSize = mfcTagData.tagBlockCount * mfcTagData.tagBytesPerBlock;
               nextSector.sectorBlockCount = mfcTagData.tagBlockCount;
               nextSector.sectorFirstBlock = sec * MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR;
               nextSector.sectorBytesPerBlock = mfcTagData.tagBytesPerBlock;
               nextSector.sectorBlockData = new String[MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR];
               for(int blk = 0; blk < MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR; blk++) {
                    nextSector.sectorBlockData[blk] = mfcTagData.mfcDumpImageData[sec * MifareClassicUtils.MFCLASSIC1K_BLOCKS_PER_SECTOR + blk];
               }
               mfcTagData.tagSectors.add(nextSector);
          }
          // load the rest of the first block (tag read-only) sector data for accounting:
          byte[] manuBlockBytes = MCTUtils.HexStringToBytes(mfcTagData.mfcDumpImageData[0]);
          byte[] uidBytes = new byte[4];
          System.arraycopy(manuBlockBytes, 0, uidBytes, 0, 4);
          mfcTagData.tagUID = MCTUtils.BytesToHexString(uidBytes);
          byte sakByte = manuBlockBytes[5];
          mfcTagData.tagSAK = MCTUtils.BytesToHexString(new byte[] { sakByte });
          byte[] atqaBytes = new byte[2];
          System.arraycopy(manuBlockBytes, 6, atqaBytes, 0, 2);
          mfcTagData.tagATQA = MCTUtils.BytesToHexString(atqaBytes);
          mfcTagData.tagATS = "Unknown ATS";
          mfcTagData.tagManufacturer = mfcTagData.mfcDumpImageData[1];
          return mfcTagData;

     }

}
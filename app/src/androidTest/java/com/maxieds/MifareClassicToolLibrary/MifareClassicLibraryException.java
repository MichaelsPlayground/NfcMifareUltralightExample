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

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

public class MifareClassicLibraryException extends Exception {

    private static final String TAG = MifareClassicLibraryException.class.getSimpleName();

    public static enum MFCLibraryExceptionType {

        GenericMFCException(0x01, "Generic MFC library error"),
        UnknownMFCException(0x02, "Unknown library exeption"),
        TagIOException(0x11, "I/O exception"),
        NFCErrorException(0x12, "Android NFC error"),
        NoKeysFoundException(0x21, "No keys found"),
        InvalidKeysException(0x22, "Invalid keys"),
        NoTagException(0x2a, "No tag"),
        UnsupportedTagException(0x23, "Unsupported tag"),
        RemovedKeyException(0x24, "Key removed"),
        PartialReadException(0x25, "Partial tag read");

        private static final Map<Integer, MFCLibraryExceptionType> ecodeToExceptionMap = new HashMap<>();
        static {
            for(MFCLibraryExceptionType mfcExcpt : values()) {
                Integer lookupEcode = Integer.valueOf(mfcExcpt.ToInteger());
                ecodeToExceptionMap.put(lookupEcode, mfcExcpt);
            }
        }

        private MFCLibraryExceptionType LookupExceptionByCode(int excptCode) {
            MFCLibraryExceptionType etype = ecodeToExceptionMap.get(excptCode);
            if(etype != null) {
                return etype;
            }
            return UnknownMFCException;
        }

        private int ecode;
        private String edesc;

        private MFCLibraryExceptionType(int excptCode, String descString) {
            ecode = excptCode;
            edesc = descString;
        }

        public int GetExceptionCode() {
            return ecode;
        }

        public int ToInteger() {
            return GetExceptionCode();
        }

        public String GetExceptionName() {
            MFCLibraryExceptionType etype = LookupExceptionByCode(ecode);
            if(etype != null) {
                return etype.name();
            }
            return null;
        }

        public String GetExceptionDescription() {
            return edesc;
        }

        public String ToString() {
            return GetExceptionName() + "::" + GetExceptionDescription();
        }

    }

    private MFCLibraryExceptionType localExceptionType;
    private String localEmsgString;
    private Intent localIntentData;

    private void SetExceptionParameters(MFCLibraryExceptionType excptType, String emsg, Intent edata) {
        localExceptionType = excptType;
        localEmsgString = excptType.ToString();
        if(emsg != null) {
            localEmsgString += " : " + emsg;
        }
        localIntentData = edata;
    }


    public MifareClassicLibraryException(MFCLibraryExceptionType excptType, String emsg) {
        super(emsg);
        SetExceptionParameters(excptType, emsg, null);
    }

    public MifareClassicLibraryException(MFCLibraryExceptionType excptType) {
        SetExceptionParameters(excptType, null, null);
    }

    public MifareClassicLibraryException(MFCLibraryExceptionType excptType, String emsg, Intent edata) {
        super(emsg);
        SetExceptionParameters(excptType, emsg, edata);
    }

    public MFCLibraryExceptionType GetExceptionType() {
        return localExceptionType;
    }

    public String ToString() {
        return localEmsgString;
    }

    public Intent GetSupportingData() {
        return localIntentData;
    }

}
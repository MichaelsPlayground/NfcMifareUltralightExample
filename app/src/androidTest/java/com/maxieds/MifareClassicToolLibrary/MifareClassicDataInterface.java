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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public interface MifareClassicDataInterface {

     void RegisterNewIntent(Intent mfcIntent);
     Context GetApplicationContext();
     Activity GetApplicationActivity();

     void PostTagScanKeyMapProgress(int position, int total);
     void PostTagScanSectorReadProgress(int position, int total);

}
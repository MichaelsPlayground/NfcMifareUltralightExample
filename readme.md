# NFC Mifare Ultralight example

## Note on this app



### Description

This app reads and writes data to NXP's Mifare Ultralight tag. It is tested with the Ultralight C and Ultralight EV1 type so 
I cannot guarantee that it works on the other type also.

The Mifare Ultralight family consists of 3 main subtypes:

1) MIFARE Ultralight: Get the datasheet MF0ICU1 here: https://www.nxp.com/docs/en/data-sheet/MF0ICU1.pdf
2) MIFARE Ultralight C: Get the datasheet here MF0ICU2: https://www.nxp.com/docs/en/data-sheet/MF0ICU2.pdf
3) MIFARE Ultralight EV1: Get the datasheet here: https://www.nxp.com/docs/en/data-sheet/MF0ULX1.pdf
 
MIFARE Ultralight AES features and hints (AN13452): https://www.nxp.com/docs/en/application-note/AN13452.pdf
MIFARE Ultralight EV1 features and hints (AN11340): https://www.nxp.com/docs/en/application-note/AN11340.pdf

All datasheets are available in the docs folder of this repository but it is always better to get one from the origin source.

https://www.asiarfid.com/how-to-choose-rfid-mifare-chip.html
 
There are 5 icons in the "Bottom Navigation Bar":

1) Home: 
2) Read: 
3) Red Value: 
4) Write: 
5) Write Value: 

As some parts of the software where "copy & paste" from Mifare Classic Tool (MCT) the license of this   
app is the same as the one for MCT: 

Icons: https://www.freeiconspng.com/images/nfc-icon

Nfc Simple PNG Transparent Background: https://www.freeiconspng.com/img/20581

<a href="https://www.freeiconspng.com/img/20581">Nfc Png Simple</a>

Minimum SDK is 21 (Android 5)

Counter on Mifare Ultralight:
```plaintext
There is no counter on Mifare Ultralight (the first version type)
```

Counter on Mifare Ultralight-C:
```plaintext
7.5.11 Counter
The MF0ICU2 features a 16-bit one-way counter, located at the first two bytes of page 
29h. The default counter value is 0000h.

The first1 valid WRITE or COMPATIBILITY WRITE to address 29h can be performed
with any value in the range between 0001h and FFFFh and corresponds to the initial
counter value. Every consecutive WRITE command, which represents the increment, can
contain values between 0001h and 000Fh. Upon such WRITE command and following
mandatory RF reset, the value written to the address 29h is added to the counter content.
After the initial write, only the lower nibble of the first data byte is used for the increment
value (0h-Fh) and the remaining part of the data is ignored. Once the counter value
reaches FFFFh and an increment is performed via a valid WRITE command, the
MF0ICU2 will reply a NAK. If the sum of counter value and increment is higher than
FFFFh, MF0ICU2 will reply a NAK and will not increment the counter.
An increment by zero (0000h) is always possible, but does not have any impact to the
counter value.
It is recommended to protect the access to the counter functionality by authentication.
```

Counter on Mifare Ultralight-EV1:
```plaintext
8.7 Counter functionality
The MF0ULx1 features three independent 24-bit one-way counters. These counters are
located in a separate part of the NVM which is not directly addressable using READ,
FAST_READ, WRITE or COMPATIBILITY_WRITE commands. The actual value can
be retrieved by using the READ_CNT command, the counters can be incremented
with the INCR_CNT command. The INCR_CNT command features anti-tearing
support, thus no undefined values originating from interrupted programing cycles are
possible. Either the value is unchanged or the correct, incremented value is correctly
programmed into the counter. The occurrence of a tearing event can be checked using
the CHECK_TEARING_EVENT command.
In the initial state, the counter values are set to 000000h.
The counters can be incremented by an arbitrary value. The incremented value is
valid immediately and does not require a RF reset or re-activation. Once counter value
reaches FFFFFFh and an increment is performed via a valid INCR_CNT command, the
MF0ULx1 replies a NAK. If the sum of the addressed counter value and the increment
value in the INCR_CNT command is higher than FFFFFFh, the MF0ULx1 replies a NAK
and does not update the respective counter.
An increment by zero (000000h) is always possible, but does not have any impact on the
counter value.
```


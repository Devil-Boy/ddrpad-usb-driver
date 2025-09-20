# DDR Dance Pad Linux Issue

This document explains an issue with using the DDR Dance Pad from [ddrpad.com](https://ddrpad.com) on Linux.

## The Problem

The DDR Dance Pad has a USB output that is compatible with Windows. However, when used with Linux, the device fails to initialize correctly. This appears to be caused by a quirk in the device's initialization communication that the standard Linux `sony` HID driver does not handle correctly.

The dance pad identifies itself as a Sony PLAYSTATION(R)3 Controller, but it fails a subsequent step in the initialization process.

## `dmesg` Output

When the device is plugged into a Linux machine, the following output can be observed in the kernel log (`dmesg`):

```
[  105.071305] usb 2-2: new full-speed USB device number 3 using ohci-pci
[  105.647327] usb 2-2: New USB device found, idVendor=054c, idProduct=0268, bcdDevice= 1.00
[  105.647334] usb 2-2: New USB device strings: Mfr=1, Product=2, SerialNumber=0
[  105.647336] usb 2-2: Product: PLAYSTATION(R)3 Controller
[  105.647338] usb 2-2: Manufacturer: Sony
[  105.709795] sony 0003:054C:0268.0002: failed to retrieve feature report 0xf2 with the Sixaxis MAC address
[  105.709947] sony 0003:054C:0268.0002: hiddev96,hidraw1: USB HID v81.11 Joystick [Sony PLAYSTATION(R)3 Controller] on usb-0000:00:06.0-2/input0
[  105.709950] sony 0003:054C:0268.0002: failed to claim input
```

The key error message is `failed to retrieve feature report 0xf2 with the Sixaxis MAC address`, which indicates that the driver is expecting a feature that is specific to an official Sixaxis controller and is not present on the dance pad. This ultimately leads to `failed to claim input`, and the device is not usable.

# DDRPad.com USB Linux Driver

![ddrpad.com soft pad image](https://web.archive.org/web/20250921040257/https://ddrpad.com/cdn/shop/files/IMG_3266-Photoroom.png-Photoroom_941718f7-f967-47e8-9e4a-4b7160ac4cb0_480x.png?v=1746740255)

I was trying
out [this DDR Pad](https://ddrpad.com/products/stepmania-soft-pad-1) ([cache](https://web.archive.org/web/20250921040257/https://ddrpad.com/products/stepmania-soft-pad-1))
to use with Project Outfox on my SteamDeck. However, it turns out that even though its USB connector works fine with
Windows' built-in driver, Linux's driver struggles due to some "quirk" in how it communicates during initialization.

You end up seeing something like this when you use `sudo dmesg -w`:

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

This is a user-space driver which:

* uses libusb (via [JavaDoesUSB](https://github.com/manuelbl/JavaDoesUSB)) to:
    * detect the DDR pad (which looks like a PlayStation 3 controller on the system)
    * temporarily override the PlayStation 3 driver
    * read physical inputs from the DDR pad
* Linux's uinput (via [linuxio4j](https://github.com/bithatch/linuxio4j)) to:
    * initialize a Virtual Gamepad on the system
    * send button presses/releases through the virtual gamepad

## Next steps

* create a udev rules to automatically enable this driver when the DDR pad is plugged into the system
* properly support multiple DDR pads
* set the polling interval based on the speed of the connection (bInterval in the endpoint descriptor is 1, but I'm not sure if that's milliseconds)

## Alternative

Instead of using this driver, you can use a Gamecube adapter ([like this one](https://www.amazon.com/gp/aw/d/B00RSXRLUE?psc=1&ref=ppx_pop_mob_b_asin_title)) and map the arrows to the a, b, x, and y buttons to avoid the "axis issue".

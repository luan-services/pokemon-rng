# Hotkey Runtime v1

Production R+SELECT trigger runtime for FireRed/LeafGreen RamScripts.

The implementation is the validated Candidate 5a architecture: 20-byte bootstrap, 48-byte aligned block table, 227-byte native installer blob, stock safety/format guards, callback rearm and VBlank supervisor.

With the 43-byte Hello payload on FR1.0 the complete script uses 409 / 995 bytes, leaving 586 bytes free.

Arbitrary payload lengths are supported: the bootstrap target is generated from the payload's actual aligned native-blob offset.

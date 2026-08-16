# Compact Runtime Candidate 5a

C5 is rejected. Its 3-byte table records made every second u16 destination field odd-aligned while the native copier still used Thumb `LDRH`. That is not a safe halfword load on ARM7TDMI and caused the installer to write resident bytes to invalid IWRAM destinations.

C5a restores the validated C4 table format (`u16 destination + u16 size`, 4-byte records) and keeps only the payload-generic bootstrap correction.

For the Hello test payload, C5a is required by the test suite to be byte-for-byte identical to validated C4. For arbitrary payload sizes, C5a generates the native-blob target after the actual payload offset/alignment is known.

Size with Hello remains 409 / 995, leaving 586 bytes free.

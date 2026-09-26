# Archived development migrations

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Historical SQL is preserved byte-for-byte as migration evidence. It is not on the Flyway runtime classpath. Original Portuguese comments remain only inside archived historical evidence; the active baseline and maintained documentation are English. The unshipped V5 was authored during foundation verification and folded into the new baseline before delivery. Do not combine this archive with the new V1 history.

| File | SHA-256 |
| --- | --- |
| [V1__Initial_Schema.sql](V1__Initial_Schema.sql) | cd907589cd411840be16f11d8d89bc4909c252f87de47851f5aa3e569254f2a0 |
| [V2__Add_Version_Column_Materials.sql](V2__Add_Version_Column_Materials.sql) | 3bacd557ec449c46e2c8ff745ef2874754136258085d7cf928cf508075c86568 |
| [V3__Add_Version_Column_Inventory_Balance.sql](V3__Add_Version_Column_Inventory_Balance.sql) | 1b58110df19a963342a201d4c25c74b95a101c437557b447496ef77297ae700a |
| [V4__Add_Destination_To_Sorted_Item_And_Pressed_Bale.sql](V4__Add_Destination_To_Sorted_Item_And_Pressed_Bale.sql) | 28af79160149d48eefbd0ed996a1bc9cbcb8cafe1cc5f546c10687cfc85e9843 |
| [V5__enforce_inventory_balance_integrity.sql](V5__enforce_inventory_balance_integrity.sql) | 00796c77859543376fc6807fb927499b8131d53272f233f784fb7729f9201438 |

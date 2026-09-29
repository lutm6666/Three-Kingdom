# Native character-art path evidence

Source binary on the local research machine:

`lib/armeabi/libgame.so`

SHA-256:

`a01a5b2d38aa2220779ebad50087b344b0040036f7e5761e4f87b577129a63fd`

Printable strings extracted from the original game binary include:

```text
character/S/ch_%s_s.png
character/M/ch_%s_m.png
character/L/ch_%s_l.png
character/W/ch_%s_w.png
character/LL/ch_%s_ll.png
```

The same native binary also exposes C++ symbols including:

```text
CharacterParameter::getSerialId()
CharacterParameter::setSerialId(...)
RFMasterDataManager::getCharacterData(...)
CharacterParameter::getCharacterParameterFromSerialId(...)
```

This is structural evidence that the character-art filename stem is populated from
the game's character serial/master identifier rather than from the public card No.

Control samples already independently verified:

```text
ch_15200025_l -> serial/master key 15200025 -> card No.102 -> 〖大斧〗徐晃
ch_35300032_l -> serial/master key 35300032 -> card No.260 -> 于禁
ch_56400035_l -> serial/master key 56400035 -> card No.350 -> 成公英
```

These controls establish that the `%s` value used by the art path is compatible
with the serial/master key used in character data.

This evidence does NOT by itself identify the remaining assets. Character names
still require an independent card/serial identity source.

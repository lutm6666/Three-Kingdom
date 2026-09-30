# Pixel-identity candidate: ch_35400027_l

## Candidate

- Original APK asset: `ch_35400027_l.png`
- Public DB card: No.157
- Public page: https://p.rakda3.net/sanpuz/b157
- Public big image: https://p.rakda3.net/sanpuz/img/bl/157.jpg
- Page identity: 〖長坂単騎駆〗趙雲

## Why this is stronger than visual resemblance

The public 600×315 card image contains the same raster character artwork as the
original transparent APK PNG: same horse, rider, weapon, clothing folds, and
green slash-effect geometry. It is the transparent source art composited over
the card-scene background.

A mechanical affine pixel test sampled 2,500 opaque/source-color points and
searched scale/translation. Best score (lower is better):

```text
bl157.jpg  30.872
bl545.jpg 141.176
bl504.jpg 142.297
bl889.jpg 143.822
bl260.jpg 144.509
bl350.jpg 146.003
bl339.jpg 147.095
bl102.jpg 147.396
bl796.jpg 147.589
```

Best transform for No.157 in this control:
`sx=0.9300 sy=0.9350 tx=2 ty=8`.

The >4× separation from wrong controls means this is not the unreliable
50×50-thumbnail dHash method.

## Status

Candidate only. Do not promote to production until an independent review checks:
1. the public page identity,
2. the image URL belongs to that card page,
3. the exact-art relation is credible,
4. no contradictory source exists.

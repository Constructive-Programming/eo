# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `cb10560050d185f563cea36f54cb2f41351112ad` · date: `2026-07-13` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.4 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.2 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.2 ± 0.1 | 10.3 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.8 ± 0.2 | 24.7 ± 0.8 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.3 ± 0.1 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.3 ± 0.1 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 162.8 ± 3.4 | — | 704.0 | — |
| `ModifyCountry` | `-` | 355.9 ± 16.8 | — | 3,448.0 | — |
| `ModifyPartner` | `-` | 411.7 ± 14.4 | — | 3,504.0 | — |
| `ReadCountry` | `-` | 184.1 ± 2.0 | — | 784.0 | — |
| `ReadPartner` | `-` | 220.5 ± 2.4 | — | 744.0 | — |
| `SliceGraftPayload` | `-` | 327.2 ± 3.4 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,757.7 ± 32.3 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,702.0 ± 7.8 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,321.0 ± 59.8 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,721.7 ± 14.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,719.4 ± 25.2 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 752.6 ± 9.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 549.1 ± 5.2 | — | 1,592.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,952.1 ± 20.9 | — | 10,802.7 | — |
| `ClickToJson` | `-` | 3,508.8 ± 56.9 | — | 5,456.0 | — |
| `WideToAvro` | `-` | 816.6 ± 12.0 | — | 7,154.7 | — |
| `WideToJson` | `-` | 608.2 ± 8.4 | — | 2,016.0 | — |
| `naiveClickToAvro` | `-` | 1,451.5 ± 18.2 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,864.2 ± 66.5 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 992.2 ± 9.2 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,975.6 ± 49.8 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 222.6 ± 0.5 | — | 984.0 | — |
| `decode_native` | `-` | 20.4 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 217.8 ± 2.6 | — | 984.0 | — |
| `encode_bridged` | `-` | 241.9 ± 8.1 | — | 1,282.7 | — |
| `encode_native` | `-` | 16.0 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 241.0 ± 3.2 | — | 1,309.3 | — |
| `fieldGet_bridged` | `-` | 90.9 ± 0.8 | — | 696.0 | — |
| `fieldGet_native` | `-` | 91.8 ± 2.5 | — | 685.3 | — |
| `rootGet_bridged` | `-` | 567.0 ± 1.3 | — | 2,352.0 | — |
| `rootGet_native` | `-` | 338.3 ± 8.1 | — | 1,376.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.9 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.7 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.3 ± 0.2 | — | 0.0 | — |
| `foldMapDirect` | `-` | 20.9 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.1 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 34.0 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 39.5 ± 0.9 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.6 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 22.5 ± 0.3 | — | 184.0 | — |
| `buildLens6` | `-` | 43.7 ± 0.5 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 22.6 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.4 ± 0.0 | — | 40.0 | — |
| `reuseLens3` | `-` | 49.0 ± 0.5 | — | 72.0 | — |
| `reuseLens6` | `-` | 135.9 ± 0.4 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 63.7 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 5,033.3 ± 13.7 | 4,736.9 ± 7.0 | 14,080.8 | 14,080.8 |
| `FoldMap` | `size=64` | 327.0 ± 4.5 | 313.6 ± 2.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.4 ± 0.0 | 21.9 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,150.4 ± 37.8 | 3,190.3 ± 63.0 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 374.2 ± 4.1 | 373.6 ± 1.2 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 48.5 ± 0.6 | 49.2 ± 1.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.6 ± 0.1 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.4 ± 0.1 | 8.8 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 32.4 ± 0.3 | 27.6 ± 0.5 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.8 ± 0.0 | 4.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.4 ± 0.0 | 3.3 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 439,002.3 ± 5,590.1 | — | 1,073,047.3 | — |
| `cModifyId` | `size=64` | 56,636.7 ± 592.4 | — | 136,430.6 | — |
| `cModifyId` | `size=8` | 9,480.2 ± 268.5 | — | 20,824.1 | — |
| `cReadId` | `size=512` | 226,971.3 ± 2,199.0 | — | 804,184.5 | — |
| `cReadId` | `size=64` | 28,752.3 ± 304.5 | — | 101,382.2 | — |
| `cReadId` | `size=8` | 4,602.8 ± 55.1 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 226,687.3 ± 2,569.8 | — | 804,352.4 | — |
| `cReadStreet` | `size=64` | 28,741.8 ± 543.8 | — | 101,550.2 | — |
| `cReadStreet` | `size=8` | 4,617.8 ± 12.5 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 444,904.6 ± 14,136.7 | — | 1,072,937.1 | — |
| `cReplaceId` | `size=64` | 56,155.8 ± 232.4 | — | 136,319.1 | — |
| `cReplaceId` | `size=8` | 9,503.7 ± 325.2 | — | 20,992.1 | — |
| `cSumPrices` | `size=512` | 360,091.4 ± 1,518.0 | — | 1,253,086.5 | — |
| `cSumPrices` | `size=64` | 45,081.1 ± 531.1 | — | 157,783.3 | — |
| `cSumPrices` | `size=8` | 6,666.4 ± 37.2 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 179.1 ± 0.7 | — | 0.1 | — |
| `jMiss` | `size=64` | 179.0 ± 0.4 | — | 0.0 | — |
| `jMiss` | `size=8` | 179.4 ± 0.9 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,028.8 ± 105.2 | — | 41,928.9 | — |
| `jModifyId` | `size=64` | 352.7 ± 3.4 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 106.1 ± 0.7 | — | 992.0 | — |
| `jReadId` | `size=512` | 39.1 ± 2.5 | — | 56.0 | — |
| `jReadId` | `size=64` | 38.2 ± 2.2 | — | 56.0 | — |
| `jReadId` | `size=8` | 37.7 ± 2.3 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 192.9 ± 3.0 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 192.6 ± 2.6 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 191.2 ± 1.6 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 2,933.2 ± 27.1 | — | 41,888.8 | — |
| `jReplaceId` | `size=64` | 349.1 ± 9.7 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 102.7 ± 1.3 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 87,966.2 ± 837.4 | — | 63,666.0 | — |
| `jSumPrices` | `size=64` | 10,697.8 ± 188.4 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,491.3 ± 58.8 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.1 | 4.1 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 39.2 ± 0.4 | 33.2 ± 0.5 | 152.0 | 176.0 |
| `Replace` | `-` | 3.6 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 54,309.4 ± 1,155.7 | — | 380,250.6 | — |
| `Modify_multiFocus` | `size=256` | 13,437.5 ± 171.9 | — | 88,422.3 | — |
| `Modify_multiFocus` | `size=32` | 1,561.2 ± 21.5 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 229.2 ± 5.4 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 38,264.4 ± 225.3 | — | 119,393.6 | — |
| `Modify_powerEach` | `size=256` | 9,336.8 ± 86.6 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,136.3 ± 7.4 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 208.9 ± 17.7 | — | 920.0 | — |
| `naive_listMap` | `size=1024` | 9,630.2 ± 386.6 | — | 65,578.5 | — |
| `naive_listMap` | `size=256` | 2,330.1 ± 101.6 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 267.6 ± 4.5 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 35.6 ± 0.2 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 68.9 ± 0.4 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 182.9 ± 2.2 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 16.3 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.2 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 41.9 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.2 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.0 ± 0.0 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 164.8 ± 0.8 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 49.3 ± 0.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,136.7 ± 28.3 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,051.4 ± 20.8 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.9 ± 0.3 | 23.1 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 64.0 ± 0.5 | 66.7 ± 0.7 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.8 ± 1.2 | 108.8 ± 0.6 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 21.1 ± 0.3 | 20.7 ± 0.5 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.2 ± 0.0 | 3.8 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 8.0 ± 0.3 | 7.5 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 40,085.5 ± 454.6 | — | 97,651.9 | — |
| `ModifyNames` | `size=64` | 5,017.8 ± 44.2 | — | 12,810.8 | — |
| `ModifyNames` | `size=8` | 685.9 ± 9.5 | — | 2,208.0 | — |
| `ModifyStreet` | `size=512` | 145.9 ± 2.3 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 145.7 ± 1.5 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 147.0 ± 3.7 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 45.6 ± 1.5 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 45.7 ± 1.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 45.3 ± 0.8 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,663.2 ± 390.8 | — | 382,771.8 | — |
| `monocleModifyNames` | `size=64` | 10,627.1 ± 688.0 | — | 39,848.4 | — |
| `monocleModifyNames` | `size=8` | 1,465.9 ± 54.7 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 58,532.9 ± 581.8 | — | 169,086.0 | — |
| `monocleModifyStreet` | `size=64` | 7,714.0 ± 339.4 | — | 20,904.3 | — |
| `monocleModifyStreet` | `size=8` | 983.5 ± 12.3 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 36,184.2 ± 168.8 | — | 69,792.4 | — |
| `monocleReadStreet` | `size=64` | 4,675.5 ± 49.9 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 548.5 ± 6.1 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 74,246.2 ± 784.4 | — | 226,301.8 | — |
| `naiveModifyNames` | `size=64` | 9,123.2 ± 479.5 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,158.9 ± 10.9 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 58,471.6 ± 380.4 | — | 169,063.5 | — |
| `naiveModifyStreet` | `size=64` | 7,256.8 ± 501.4 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 986.6 ± 7.1 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 36,392.3 ± 177.9 | — | 69,792.4 | — |
| `naiveReadStreet` | `size=64` | 4,672.2 ± 9.4 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 544.3 ± 1.3 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 247,311.4 ± 5,502.6 | — | 646,850.8 | — |
| `Names` | `size=64` | 30,905.9 ± 499.9 | — | 82,955.1 | — |
| `Names` | `size=8` | 4,213.7 ± 113.3 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 273,976.7 ± 7,510.9 | — | 724,591.8 | — |
| `NamesIor` | `size=64` | 32,535.0 ± 498.9 | — | 91,160.3 | — |
| `NamesIor` | `size=8` | 4,254.3 ± 47.2 | — | 12,032.1 | — |
| `Street` | `size=512` | 1,066.2 ± 29.0 | — | 2,968.8 | — |
| `Street` | `size=64` | 1,045.1 ± 9.4 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,059.0 ± 21.7 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,069.0 ± 12.7 | — | 2,984.8 | — |
| `StreetIor` | `size=64` | 1,068.6 ± 21.9 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,055.0 ± 15.3 | — | 2,984.0 | — |
| `directNames` | `size=512` | 245,855.2 ± 3,651.6 | — | 613,993.6 | — |
| `directNames` | `size=64` | 29,644.5 ± 167.6 | — | 77,729.9 | — |
| `directNames` | `size=8` | 4,048.4 ± 59.7 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,021.3 ± 19.1 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,023.1 ± 17.1 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 1,024.3 ± 20.6 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 244,842.0 ± 3,218.8 | — | 613,992.8 | — |
| `hcursorNames` | `size=64` | 30,076.5 ± 323.0 | — | 77,786.9 | — |
| `hcursorNames` | `size=8` | 4,124.6 ± 44.6 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,088.7 ± 17.8 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,086.0 ± 18.2 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,068.5 ± 5.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 240,200.8 ± 3,454.3 | — | 1,121,777.2 | — |
| `monocleNames` | `size=64` | 25,831.4 ± 304.1 | — | 132,780.4 | — |
| `monocleNames` | `size=8` | 3,832.7 ± 50.5 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 186,098.7 ± 2,519.5 | — | 908,034.4 | — |
| `monocleStreet` | `size=64` | 22,385.9 ± 45.5 | — | 113,799.3 | — |
| `monocleStreet` | `size=8` | 3,287.9 ± 65.2 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 211,553.2 ± 8,553.5 | — | 965,278.1 | — |
| `naiveNames` | `size=64` | 24,242.8 ± 243.3 | — | 120,826.2 | — |
| `naiveNames` | `size=8` | 3,467.0 ± 41.7 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 186,431.7 ± 4,689.4 | — | 908,034.6 | — |
| `naiveStreet` | `size=64` | 22,406.8 ± 242.7 | — | 113,802.0 | — |
| `naiveStreet` | `size=8` | 3,265.8 ± 28.9 | — | 17,056.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,220.9 ± 19.3 | — | 42,026.9 | — |
| `ModifyStreet` | `size=64` | 522.5 ± 4.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 283.1 ± 0.9 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 191.1 ± 1.0 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 191.3 ± 1.2 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 191.5 ± 2.7 | — | 128.0 | — |
| `SumPrices` | `size=512` | 88,217.1 ± 455.4 | — | 63,719.5 | — |
| `SumPrices` | `size=64` | 10,846.6 ± 37.6 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,447.7 ± 9.5 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 173,150.3 ± 1,588.9 | — | 333,506.1 | — |
| `monocleModifyStreet` | `size=64` | 21,713.2 ± 448.9 | — | 30,114.2 | — |
| `monocleModifyStreet` | `size=8` | 3,491.3 ± 13.8 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 98,755.0 ± 572.9 | — | 193,268.5 | — |
| `monocleReadStreet` | `size=64` | 12,477.7 ± 93.8 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,892.8 ± 14.1 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 471,558.3 ± 3,587.1 | — | 1,190,820.2 | — |
| `monocleSumPrices` | `size=64` | 17,009.1 ± 74.6 | — | 47,417.7 | — |
| `monocleSumPrices` | `size=8` | 2,584.4 ± 29.2 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 173,880.0 ± 2,706.5 | — | 333,608.5 | — |
| `naiveModifyStreet` | `size=64` | 21,610.0 ± 81.0 | — | 30,090.2 | — |
| `naiveModifyStreet` | `size=8` | 3,525.7 ± 29.6 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 99,249.0 ± 1,102.1 | — | 193,268.9 | — |
| `naiveReadStreet` | `size=64` | 12,486.0 ± 52.5 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,898.2 ± 22.0 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 103,788.5 ± 1,209.9 | — | 230,160.8 | — |
| `naiveSumPrices` | `size=64` | 13,099.6 ± 220.8 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 1,971.8 ± 16.8 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 42,604.6 ± 1,358.7 | — | 459.0 | — |
| `nativeReadStreet` | `size=64` | 5,290.3 ± 129.3 | — | 424.6 | — |
| `nativeReadStreet` | `size=8` | 863.9 ± 4.2 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 68,644.3 ± 283.3 | — | 86,273.5 | — |
| `nativeSumPrices` | `size=64` | 8,500.7 ± 44.1 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,236.7 ± 17.6 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 134,138.9 ± 1,823.8 | — | 624,393.6 | — |
| `TransformDeep` | `n=512` | 13,599.8 ± 77.1 | — | 57,361.4 | — |
| `TransformDeep` | `n=64` | 1,576.8 ± 11.7 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 157,655.3 ± 5,243.7 | 183,925.3 ± 2,668.8 | 655,378.7 | 753,749.8 |
| `TransformExpr` | `n=512` | 18,780.7 ± 138.4 | 16,665.4 ± 116.9 | 81,825.9 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,306.4 ± 5.5 | 2,836.4 ± 23.4 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 111,036.8 ± 2,228.6 | — | 786,584.8 | — |
| `UniverseDeep` | `n=512` | 16,179.3 ± 237.9 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,945.0 ± 19.6 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 102,642.7 ± 251.2 | 1,791,856.0 ± 41,716.4 | 786,386.7 | 4,752,518.8 |
| `UniverseExpr` | `n=512` | 15,540.6 ± 140.1 | 179,772.7 ± 3,424.6 | 98,185.6 | 483,202.3 |
| `UniverseExpr` | `n=64` | 1,880.3 ± 12.8 | 16,707.2 ± 374.8 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 195,530.2 ± 6,566.2 | 2,004,005.1 ± 91,039.1 | 786,454.3 | 6,489,000.9 |
| `UniverseJson` | `n=512` | 21,447.5 ± 187.1 | 209,132.5 ± 650.7 | 98,186.2 | 699,917.3 |
| `UniverseJson` | `n=64` | 2,519.0 ± 19.9 | 20,693.1 ± 237.7 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 45,003.4 ± 544.0 | — | 163,888.8 | — |
| `visitorTransformDeep` | `n=512` | 4,110.0 ± 80.6 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 450.5 ± 1.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 73,410.6 ± 761.7 | — | 360,477.4 | — |
| `visitorTransformExpr` | `n=512` | 9,105.0 ± 163.3 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,129.4 ± 15.1 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 60,057.7 ± 1,102.4 | — | 196,707.7 | — |
| `visitorUniverseDeep` | `n=512` | 7,435.8 ± 135.9 | — | 24,632.8 | — |
| `visitorUniverseDeep` | `n=64` | 843.4 ± 19.3 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,482.9 ± 91.1 | — | 196,656.4 | — |
| `visitorUniverseExpr` | `n=512` | 7,000.2 ± 154.5 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 827.1 ± 6.8 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 130,246.8 ± 3,470.6 | — | 294,990.8 | — |
| `visitorUniverseJson` | `n=512` | 14,276.0 ± 49.8 | — | 36,849.5 | — |
| `visitorUniverseJson` | `n=64` | 2,172.6 ± 273.4 | — | 4,928.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 14,888.3 ± 188.9 | — | 41,456.6 | — |
| `Modify_powerEach` | `size=16` | 292.5 ± 3.5 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,557.5 ± 36.7 | — | 10,712.6 | — |
| `Modify_powerEach` | `size=4` | 133.3 ± 6.2 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 58,138.4 ± 1,184.9 | — | 164,433.8 | — |
| `Modify_powerEach` | `size=64` | 934.2 ± 16.6 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 58,503.5 ± 568.9 | — | 279,433.2 | — |
| `monocle_powerEach` | `size=16` | 554.3 ± 32.6 | — | 3,725.3 | — |
| `monocle_powerEach` | `size=256` | 21,542.0 ± 224.3 | — | 107,339.5 | — |
| `monocle_powerEach` | `size=4` | 237.0 ± 7.5 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 185,315.0 ± 2,662.9 | — | 967,871.9 | — |
| `monocle_powerEach` | `size=64` | 2,187.6 ± 97.5 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,832.2 ± 28.9 | — | 28,730.9 | — |
| `naive_powerEach` | `size=16` | 110.6 ± 0.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,704.5 ± 28.4 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 28.6 ± 0.4 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,211.2 ± 186.8 | — | 114,784.1 | — |
| `naive_powerEach` | `size=64` | 427.5 ± 4.5 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 65,137.6 ± 499.4 | — | 210,928.4 | — |
| `Modify_nested` | `size=16` | 1,625.0 ± 32.3 | — | 5,256.1 | — |
| `Modify_nested` | `size=256` | 18,248.8 ± 742.1 | — | 54,219.5 | — |
| `Modify_nested` | `size=4` | 774.7 ± 6.4 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,320.1 ± 113.1 | — | 15,019.2 | — |
| `monocle_nested` | `size=1024` | 249,944.9 ± 5,697.3 | — | 1,118,879.6 | — |
| `monocle_nested` | `size=16` | 2,542.1 ± 42.6 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 92,761.6 ± 1,428.7 | — | 430,212.6 | — |
| `monocle_nested` | `size=4` | 1,252.6 ± 110.8 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 8,733.1 ± 153.4 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 22,525.8 ± 616.7 | — | 115,075.2 | — |
| `naive_nested` | `size=16` | 412.6 ± 7.7 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,300.2 ± 99.4 | — | 29,019.5 | — |
| `naive_nested` | `size=4` | 146.5 ± 4.5 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,478.6 ± 27.3 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,756.3 ± 207.2 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 30,714.2 ± 950.2 | — | 104,803.3 | — |
| `Modify_sparse` | `size=32` | 542.7 ± 6.7 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,603.5 ± 140.0 | — | 24,903.4 | — |
| `Modify_sparse` | `size=8` | 175.8 ± 4.2 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,845.7 ± 49.3 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 95,574.7 ± 562.6 | — | 475,347.6 | — |
| `monocle_sparse` | `size=32` | 976.6 ± 15.5 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,650.5 ± 138.2 | — | 156,442.7 | — |
| `monocle_sparse` | `size=8` | 274.9 ± 0.5 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 323.3 ± 2.0 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,587.9 ± 84.0 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 83.8 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,293.8 ± 10.7 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 26.2 ± 0.4 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.4 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.6 ± 0.0 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.9 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.4 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 122,414.2 ± 1,694.6 | — | 786,296.9 | — |
| `Cata` | `-` | 87,323.9 ± 402.2 | — | 197,568.6 | — |
| `Hylo` | `-` | 84,935.4 ± 990.4 | — | 295,848.6 | — |
| `drosteAna` | `-` | 51,972.7 ± 2,974.0 | — | 327,632.4 | — |
| `drosteCata` | `-` | 47,020.5 ± 165.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,990.6 ± 884.0 | — | 328,640.4 | — |
| `handAna` | `-` | 21,813.9 ± 380.8 | — | 163,816.2 | — |
| `handCata` | `-` | 12,973.4 ± 23.5 | — | 0.1 | — |
| `handHylo` | `-` | 12,127.7 ± 49.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.1 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.3 ± 0.2 | 26.7 ± 0.5 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.5 ± 0.8 | 53.0 ± 0.9 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,877.7 ± 105.0 | 33,844.0 ± 2,009.8 | 39,001.2 | 176,902.1 |
| `Modify` | `size=64` | 984.2 ± 3.3 | 1,817.0 ± 23.8 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 116.3 ± 2.7 | 243.5 ± 1.6 | 728.0 | 1,936.0 |


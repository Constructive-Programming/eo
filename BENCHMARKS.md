# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `cbf2db802fe4af9df9fcc9bca7767d9d4b51c6e8` · date: `2026-07-13` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.3 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.3 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.9 ± 0.1 | 10.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.9 ± 0.7 | 23.0 ± 0.2 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.2 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 163.6 ± 4.0 | — | 704.0 | — |
| `ModifyCountry` | `-` | 398.8 ± 28.3 | — | 3,450.7 | — |
| `ModifyPartner` | `-` | 454.5 ± 5.4 | — | 3,504.0 | — |
| `ReadCountry` | `-` | 178.0 ± 3.1 | — | 784.0 | — |
| `ReadPartner` | `-` | 220.4 ± 4.3 | — | 744.0 | — |
| `SliceGraftPayload` | `-` | 326.0 ± 8.1 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,713.0 ± 51.6 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,671.5 ± 31.8 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,122.7 ± 139.8 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,715.8 ± 21.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,726.5 ± 8.4 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 765.1 ± 10.9 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 598.9 ± 26.2 | — | 1,592.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,828.9 ± 40.8 | — | 10,813.4 | — |
| `ClickToJson` | `-` | 3,433.0 ± 73.8 | — | 5,456.0 | — |
| `WideToAvro` | `-` | 990.9 ± 32.9 | — | 7,144.0 | — |
| `WideToJson` | `-` | 671.4 ± 20.5 | — | 2,016.0 | — |
| `naiveClickToAvro` | `-` | 1,665.5 ± 152.5 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,763.4 ± 11.4 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,009.6 ± 5.5 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,965.5 ± 38.6 | — | 4,376.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.7 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 32.8 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.5 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.4 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.1 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 41.6 ± 0.4 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.6 ± 0.3 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.7 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 46.3 ± 0.4 | — | 72.0 | — |
| `reuseLens6` | `-` | 134.4 ± 1.3 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.7 ± 1.0 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,664.2 ± 192.3 | 4,486.9 ± 46.5 | 14,080.8 | 14,080.7 |
| `FoldMap` | `size=64` | 325.2 ± 0.6 | 307.9 ± 1.6 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.1 ± 0.0 | 20.4 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,782.2 ± 4.5 | 6,060.9 ± 5,170.5 | 12,312.5 | 12,313.0 |
| `FoldPrices` | `size=64` | 352.0 ± 4.8 | 354.5 ± 2.4 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.4 ± 0.3 | 44.7 ± 0.3 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 17.0 ± 0.1 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.2 ± 0.2 | 25.4 ± 0.9 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.6 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 417,026.9 ± 9,939.6 | — | 1,073,028.2 | — |
| `cModifyId` | `size=64` | 52,960.1 ± 562.4 | — | 136,398.3 | — |
| `cModifyId` | `size=8` | 8,811.0 ± 71.5 | — | 20,816.1 | — |
| `cReadId` | `size=512` | 220,937.0 ± 4,277.8 | — | 804,182.8 | — |
| `cReadId` | `size=64` | 27,602.4 ± 662.5 | — | 101,381.2 | — |
| `cReadId` | `size=8` | 4,649.1 ± 193.1 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 217,354.5 ± 2,088.4 | — | 804,349.8 | — |
| `cReadStreet` | `size=64` | 27,373.8 ± 518.6 | — | 101,548.8 | — |
| `cReadStreet` | `size=8` | 4,605.6 ± 179.6 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 411,652.9 ± 5,400.2 | — | 1,072,951.2 | — |
| `cReplaceId` | `size=64` | 52,712.3 ± 240.8 | — | 136,318.0 | — |
| `cReplaceId` | `size=8` | 8,797.6 ± 53.4 | — | 20,752.1 | — |
| `cSumPrices` | `size=512` | 348,050.2 ± 1,987.9 | — | 1,253,090.9 | — |
| `cSumPrices` | `size=64` | 43,518.9 ± 682.8 | — | 157,825.0 | — |
| `cSumPrices` | `size=8` | 6,635.7 ± 98.1 | — | 22,920.1 | — |
| `jMiss` | `size=512` | 164.1 ± 0.5 | — | 0.0 | — |
| `jMiss` | `size=64` | 166.2 ± 2.6 | — | 0.0 | — |
| `jMiss` | `size=8` | 164.8 ± 1.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,412.2 ± 36.1 | — | 41,921.0 | — |
| `jModifyId` | `size=64` | 433.6 ± 4.2 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 105.2 ± 0.4 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.9 ± 1.0 | — | 64.0 | — |
| `jReadId` | `size=64` | 34.7 ± 1.0 | — | 48.0 | — |
| `jReadId` | `size=8` | 35.4 ± 0.4 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 187.6 ± 11.9 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 179.0 ± 1.7 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 179.1 ± 1.6 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,381.2 ± 65.6 | — | 41,889.0 | — |
| `jReplaceId` | `size=64` | 426.3 ± 7.0 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 104.8 ± 1.2 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 84,546.5 ± 1,857.7 | — | 63,666.8 | — |
| `jSumPrices` | `size=64` | 10,473.4 ± 145.8 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,452.9 ± 47.5 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.4 ± 0.1 | 31.3 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.2 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 53,454.1 ± 499.8 | — | 380,250.4 | — |
| `Modify_multiFocus` | `size=256` | 12,826.9 ± 33.1 | — | 88,400.9 | — |
| `Modify_multiFocus` | `size=32` | 1,555.6 ± 15.0 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 252.7 ± 8.8 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 35,831.5 ± 213.9 | — | 119,393.0 | — |
| `Modify_powerEach` | `size=256` | 8,671.9 ± 71.0 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,101.3 ± 16.1 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 209.1 ± 14.0 | — | 920.0 | — |
| `naive_listMap` | `size=1024` | 8,344.4 ± 111.2 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,103.9 ± 81.5 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 256.1 ± 10.4 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.0 ± 0.3 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 67.1 ± 0.4 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 165.5 ± 2.5 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.3 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.7 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.4 ± 0.4 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.3 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.7 ± 0.4 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.4 ± 0.7 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.7 ± 0.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,257.4 ± 15.8 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,185.7 ± 16.0 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.9 ± 0.1 | 22.4 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 61.8 ± 0.7 | 71.3 ± 1.6 | 160.0 | 304.0 |
| `Modify_6` | `-` | 152.9 ± 2.2 | 115.6 ± 1.1 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.7 ± 0.0 | 20.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.0 ± 0.0 | 6.9 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 40,744.1 ± 6,678.0 | — | 97,641.2 | — |
| `ModifyNames` | `size=64` | 4,705.5 ± 50.6 | — | 12,810.8 | — |
| `ModifyNames` | `size=8` | 659.6 ± 3.3 | — | 2,200.0 | — |
| `ModifyStreet` | `size=512` | 134.5 ± 1.4 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 132.8 ± 0.4 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 133.2 ± 0.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 40.7 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 40.7 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 40.7 ± 0.5 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 102,940.8 ± 795.0 | — | 382,772.6 | — |
| `monocleModifyNames` | `size=64` | 10,048.3 ± 361.9 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,531.1 ± 8.9 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 55,263.1 ± 301.1 | — | 169,085.8 | — |
| `monocleModifyStreet` | `size=64` | 7,242.5 ± 25.4 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 1,038.6 ± 16.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,783.7 ± 268.9 | — | 69,789.5 | — |
| `monocleReadStreet` | `size=64` | 4,255.6 ± 103.9 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 516.3 ± 10.8 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 73,524.6 ± 5,855.7 | — | 226,302.0 | — |
| `naiveModifyNames` | `size=64` | 8,598.1 ± 446.8 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,213.7 ± 4.3 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,499.4 ± 643.3 | — | 169,061.9 | — |
| `naiveModifyStreet` | `size=64` | 6,951.3 ± 455.8 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,033.0 ± 6.6 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,994.2 ± 322.1 | — | 69,789.3 | — |
| `naiveReadStreet` | `size=64` | 4,226.1 ± 26.0 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 514.1 ± 8.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 274,152.1 ± 2,968.0 | — | 642,771.7 | — |
| `Names` | `size=64` | 34,538.8 ± 986.5 | — | 82,939.6 | — |
| `Names` | `size=8` | 4,706.1 ± 88.9 | — | 11,360.1 | — |
| `NamesIor` | `size=512` | 302,007.6 ± 6,014.8 | — | 720,514.1 | — |
| `NamesIor` | `size=64` | 37,751.3 ± 753.2 | — | 90,630.8 | — |
| `NamesIor` | `size=8` | 5,016.1 ± 104.6 | — | 12,032.1 | — |
| `Street` | `size=512` | 1,150.3 ± 28.8 | — | 2,968.9 | — |
| `Street` | `size=64` | 1,193.8 ± 19.2 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,182.1 ± 25.6 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,180.6 ± 18.2 | — | 2,984.9 | — |
| `StreetIor` | `size=64` | 1,196.4 ± 25.4 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,176.7 ± 19.5 | — | 2,984.0 | — |
| `directNames` | `size=512` | 272,552.4 ± 7,051.6 | — | 614,018.5 | — |
| `directNames` | `size=64` | 32,780.6 ± 482.8 | — | 77,742.8 | — |
| `directNames` | `size=8` | 4,477.6 ± 71.4 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,138.0 ± 9.5 | — | 2,752.9 | — |
| `directStreet` | `size=64` | 1,096.9 ± 17.3 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,109.3 ± 7.7 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 258,290.4 ± 6,187.8 | — | 609,929.6 | — |
| `hcursorNames` | `size=64` | 32,970.4 ± 717.3 | — | 77,793.2 | — |
| `hcursorNames` | `size=8` | 4,528.5 ± 89.5 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,187.9 ± 13.6 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,204.8 ± 27.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,200.2 ± 14.0 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 236,919.0 ± 995.0 | — | 1,121,767.2 | — |
| `monocleNames` | `size=64` | 25,768.4 ± 117.3 | — | 132,749.0 | — |
| `monocleNames` | `size=8` | 4,061.8 ± 35.8 | — | 19,509.4 | — |
| `monocleStreet` | `size=512` | 189,680.0 ± 2,371.8 | — | 908,031.4 | — |
| `monocleStreet` | `size=64` | 22,798.1 ± 94.9 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,444.9 ± 14.1 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 204,015.2 ± 1,161.5 | — | 965,273.1 | — |
| `naiveNames` | `size=64` | 24,352.2 ± 92.3 | — | 120,842.2 | — |
| `naiveNames` | `size=8` | 3,627.1 ± 11.2 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 193,034.6 ± 3,267.0 | — | 908,033.7 | — |
| `naiveStreet` | `size=64` | 22,733.5 ± 160.4 | — | 113,770.0 | — |
| `naiveStreet` | `size=8` | 3,460.7 ± 19.3 | — | 17,050.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,727.6 ± 26.4 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 593.5 ± 10.3 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 294.6 ± 7.8 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 182.9 ± 5.5 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 182.3 ± 3.6 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 178.8 ± 1.5 | — | 128.0 | — |
| `SumPrices` | `size=512` | 85,707.4 ± 2,214.3 | — | 63,720.4 | — |
| `SumPrices` | `size=64` | 10,662.4 ± 56.4 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,417.1 ± 19.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 166,458.7 ± 811.2 | — | 333,619.1 | — |
| `monocleModifyStreet` | `size=64` | 20,532.1 ± 40.2 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,403.4 ± 12.6 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,077.0 ± 486.5 | — | 193,265.4 | — |
| `monocleReadStreet` | `size=64` | 12,362.9 ± 207.7 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,925.3 ± 16.6 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 447,094.0 ± 1,915.8 | — | 1,190,798.9 | — |
| `monocleSumPrices` | `size=64` | 16,382.6 ± 501.5 | — | 46,903.0 | — |
| `monocleSumPrices` | `size=8` | 2,651.7 ± 56.7 | — | 6,680.1 | — |
| `naiveModifyStreet` | `size=512` | 166,859.8 ± 926.1 | — | 333,604.1 | — |
| `naiveModifyStreet` | `size=64` | 20,677.2 ± 206.3 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,416.2 ± 20.2 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 96,727.1 ± 2,944.0 | — | 193,266.8 | — |
| `naiveReadStreet` | `size=64` | 12,268.8 ± 209.3 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,930.4 ± 15.2 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 99,833.8 ± 479.9 | — | 230,157.5 | — |
| `naiveSumPrices` | `size=64` | 12,884.2 ± 173.0 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,014.7 ± 10.6 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 40,680.7 ± 395.9 | — | 457.2 | — |
| `nativeReadStreet` | `size=64` | 4,752.9 ± 14.6 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 806.5 ± 6.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,859.3 ± 1,157.7 | — | 86,269.8 | — |
| `nativeSumPrices` | `size=64` | 8,020.5 ± 42.9 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,199.2 ± 19.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 132,166.4 ± 899.7 | — | 624,392.2 | — |
| `TransformDeep` | `n=512` | 13,009.5 ± 48.0 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,612.4 ± 8.1 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 147,479.8 ± 4,417.7 | 176,834.8 ± 2,463.2 | 655,371.3 | 753,744.7 |
| `TransformExpr` | `n=512` | 18,241.5 ± 145.2 | 16,022.2 ± 138.1 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,248.4 ± 20.8 | 2,713.3 ± 13.6 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 100,872.9 ± 1,537.2 | — | 786,577.4 | — |
| `UniverseDeep` | `n=512` | 15,336.9 ± 48.9 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,876.7 ± 19.3 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 98,657.4 ± 1,176.3 | 1,739,297.4 ± 75,261.6 | 786,383.8 | 4,883,536.9 |
| `UniverseExpr` | `n=512` | 14,620.4 ± 110.7 | 173,630.0 ± 7,202.1 | 98,185.5 | 483,201.7 |
| `UniverseExpr` | `n=64` | 1,763.7 ± 21.8 | 16,344.2 ± 116.9 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 184,222.3 ± 1,922.3 | 1,961,087.8 ± 66,352.2 | 786,446.1 | 6,882,137.9 |
| `UniverseJson` | `n=512` | 20,301.6 ± 78.4 | 211,397.1 ± 1,291.4 | 98,186.1 | 699,917.6 |
| `UniverseJson` | `n=64` | 2,472.2 ± 14.3 | 20,820.6 ± 349.6 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 34,825.0 ± 193.5 | — | 163,881.3 | — |
| `visitorTransformDeep` | `n=512` | 4,120.4 ± 43.7 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 432.8 ± 4.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 67,638.8 ± 674.7 | — | 360,473.2 | — |
| `visitorTransformExpr` | `n=512` | 8,309.3 ± 29.7 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,042.0 ± 16.7 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,024.6 ± 780.4 | — | 196,704.8 | — |
| `visitorUniverseDeep` | `n=512` | 6,873.8 ± 70.0 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 806.2 ± 8.9 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,297.6 ± 476.2 | — | 196,656.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,836.9 ± 53.4 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 835.6 ± 1.2 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 142,047.5 ± 1,487.2 | — | 294,999.4 | — |
| `visitorUniverseJson` | `n=512` | 13,801.7 ± 53.8 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,268.6 ± 257.1 | — | 5,264.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,859.8 ± 59.5 | — | 41,438.6 | — |
| `Modify_powerEach` | `size=16` | 296.7 ± 3.4 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,310.2 ± 26.3 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 149.7 ± 12.3 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 57,083.6 ± 1,114.2 | — | 164,430.4 | — |
| `Modify_powerEach` | `size=64` | 897.3 ± 4.8 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 59,798.6 ± 977.8 | — | 279,434.2 | — |
| `monocle_powerEach` | `size=16` | 619.3 ± 14.8 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 22,356.5 ± 641.7 | — | 107,331.6 | — |
| `monocle_powerEach` | `size=4` | 252.9 ± 7.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 184,532.9 ± 4,238.9 | — | 967,870.6 | — |
| `monocle_powerEach` | `size=64` | 2,079.8 ± 13.6 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,527.4 ± 10.9 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 101.4 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,614.7 ± 5.2 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.0 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,388.6 ± 82.1 | — | 114,782.7 | — |
| `naive_powerEach` | `size=64` | 394.2 ± 0.6 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 66,241.6 ± 2,401.0 | — | 210,978.9 | — |
| `Modify_nested` | `size=16` | 1,686.6 ± 4.7 | — | 5,221.4 | — |
| `Modify_nested` | `size=256` | 16,483.2 ± 696.5 | — | 54,170.3 | — |
| `Modify_nested` | `size=4` | 811.1 ± 3.8 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,342.4 ± 19.4 | — | 15,101.8 | — |
| `monocle_nested` | `size=1024` | 253,733.3 ± 2,444.8 | — | 1,118,886.9 | — |
| `monocle_nested` | `size=16` | 2,888.7 ± 14.9 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 95,084.5 ± 2,324.4 | — | 430,213.2 | — |
| `monocle_nested` | `size=4` | 1,330.8 ± 116.1 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,949.4 ± 180.9 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 21,697.0 ± 941.4 | — | 115,073.6 | — |
| `naive_nested` | `size=16` | 388.9 ± 6.4 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,821.3 ± 26.3 | — | 29,019.2 | — |
| `naive_nested` | `size=4` | 134.4 ± 3.1 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,414.4 ± 12.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,556.0 ± 22.3 | — | 4,933.4 | — |
| `Modify_sparse` | `size=2048` | 29,123.2 ± 309.7 | — | 104,806.5 | — |
| `Modify_sparse` | `size=32` | 521.5 ± 11.5 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,198.0 ± 55.3 | — | 24,903.3 | — |
| `Modify_sparse` | `size=8` | 175.4 ± 0.9 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,687.9 ± 9.1 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 100,446.7 ± 728.0 | — | 476,033.7 | — |
| `monocle_sparse` | `size=32` | 1,007.1 ± 13.6 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 32,142.4 ± 794.3 | — | 156,443.3 | — |
| `monocle_sparse` | `size=8` | 331.5 ± 5.6 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 326.8 ± 1.1 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,453.0 ± 51.9 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 84.1 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,294.0 ± 4.7 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.3 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.2 ± 0.1 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.3 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.0 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.7 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 143,244.0 ± 2,610.7 | — | 786,297.0 | — |
| `Cata` | `-` | 84,784.3 ± 319.4 | — | 197,568.6 | — |
| `Hylo` | `-` | 84,161.9 ± 492.0 | — | 295,848.6 | — |
| `drosteAna` | `-` | 54,805.7 ± 3,879.4 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,861.0 ± 329.0 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,223.4 ± 186.7 | — | 328,640.5 | — |
| `handAna` | `-` | 20,221.2 ± 592.3 | — | 163,816.1 | — |
| `handCata` | `-` | 13,150.1 ± 108.0 | — | 0.1 | — |
| `handHylo` | `-` | 11,374.0 ± 212.1 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.3 ± 0.1 | 2.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.5 ± 0.1 | 25.9 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.0 ± 0.1 | 59.1 ± 0.5 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.1 ± 0.1 | 3.1 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,102.7 ± 87.8 | 35,885.5 ± 478.1 | 39,001.1 | 176,924.6 |
| `Modify` | `size=64` | 958.6 ± 4.0 | 1,769.6 ± 5.8 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 110.0 ± 0.5 | 291.8 ± 5.4 | 728.0 | 1,936.0 |


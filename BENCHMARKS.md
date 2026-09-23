# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `11287866a61c5d119b28291c0b87034c1b14b25e` · date: `2026-09-23` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 8.3 ± 0.3 | 5.3 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 17.9 ± 0.4 | 14.8 ± 0.5 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 84.6 ± 3.9 | — | 720.0 | — |
| `ModifyCountry` | `-` | 378.3 ± 23.4 | — | 3,200.0 | — |
| `ModifyPartner` | `-` | 395.6 ± 10.9 | — | 3,256.0 | — |
| `ReadCountry` | `-` | 115.3 ± 7.3 | — | 520.0 | — |
| `ReadPartner` | `-` | 128.6 ± 5.0 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 185.6 ± 3.6 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 1,639.3 ± 39.3 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 1,877.3 ± 55.7 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 2,975.9 ± 64.9 | — | 14,088.0 | — |
| `naiveReadCountry` | `-` | 967.8 ± 12.8 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,271.2 ± 53.5 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 527.3 ± 10.8 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 353.4 ± 6.3 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 223.7 ± 4.8 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 256.8 ± 7.0 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 872.7 ± 48.9 | — | 3,677.3 | — |
| `freshDecodeRecord` | `-` | 814.3 ± 18.0 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,250.2 ± 127.8 | — | 9,368.0 | — |
| `ClickToJson` | `-` | 1,848.2 ± 58.1 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 741.1 ± 24.9 | — | 6,584.0 | — |
| `WideToJson` | `-` | 349.9 ± 13.0 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 995.4 ± 53.8 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 1,843.6 ± 51.8 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 647.9 ± 14.4 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,103.4 ± 27.7 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 118.9 ± 5.2 | — | 880.0 | — |
| `decode_native` | `-` | 10.4 ± 0.5 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 113.2 ± 4.0 | — | 880.0 | — |
| `encode_bridged` | `-` | 126.1 ± 4.6 | — | 1,224.0 | — |
| `encode_native` | `-` | 7.9 ± 0.2 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 130.6 ± 5.7 | — | 1,229.3 | — |
| `fieldGet_bridged` | `-` | 60.0 ± 2.3 | — | 437.3 | — |
| `fieldGet_native` | `-` | 59.4 ± 1.1 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 212.3 ± 7.8 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 95.1 ± 4.6 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 13.0 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 14.3 ± 0.3 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 12.9 ± 0.3 | — | 0.0 | — |
| `foldMapDirect` | `-` | 12.7 ± 0.3 | — | 0.0 | — |
| `getCap` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 2.2 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 19.2 ± 0.8 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 20.9 ± 0.7 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 3.0 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 2.5 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 2.2 ± 0.2 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 290.8 ± 14.2 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 291.8 ± 13.8 | — | 744.0 | — |
| `encode_hand` | `-` | 85.5 ± 1.1 | — | 768.0 | — |
| `encode_positional` | `-` | 3,307.2 ± 91.5 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 3,946.5 ± 99.0 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 2.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 8.0 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 14.7 ± 0.3 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 8.0 ± 0.3 | — | 184.0 | — |
| `reuseLeaf` | `-` | 1.5 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 9.2 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 25.5 ± 0.7 | — | 72.0 | — |
| `reuseLens6` | `-` | 74.8 ± 1.3 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 33.5 ± 1.6 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 2,455.7 ± 164.2 | 2,309.8 ± 111.1 | 14,080.4 | 14,080.4 |
| `FoldMap` | `size=64` | 208.4 ± 5.1 | 204.7 ± 2.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 13.0 ± 0.3 | 12.9 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 1,895.8 ± 155.5 | 1,886.2 ± 121.4 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 185.2 ± 3.8 | 187.9 ± 7.3 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 22.0 ± 0.4 | 22.3 ± 0.6 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.0 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 1.8 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 1.6 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 1.7 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.3 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 1.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.5 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 9.1 ± 0.1 | 4.2 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 18.8 ± 0.6 | 12.7 ± 0.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.0 ± 0.0 | 2.0 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 1.5 ± 0.0 | 1.6 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 224,705.8 ± 5,921.2 | — | 1,066,655.9 | — |
| `cModifyId` | `size=64` | 30,026.0 ± 851.5 | — | 136,259.9 | — |
| `cModifyId` | `size=8` | 4,887.2 ± 107.2 | — | 20,704.0 | — |
| `cReadId` | `size=512` | 109,212.1 ± 2,069.6 | — | 797,912.7 | — |
| `cReadId` | `size=64` | 14,016.2 ± 335.7 | — | 101,288.4 | — |
| `cReadId` | `size=8` | 2,195.9 ± 72.1 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 109,422.5 ± 1,643.7 | — | 797,912.0 | — |
| `cReadStreet` | `size=64` | 13,953.9 ± 419.1 | — | 101,288.4 | — |
| `cReadStreet` | `size=8` | 2,234.9 ± 66.9 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 221,068.1 ± 3,738.3 | — | 1,066,630.9 | — |
| `cReplaceId` | `size=64` | 29,220.1 ± 601.7 | — | 136,211.9 | — |
| `cReplaceId` | `size=8` | 4,888.2 ± 150.1 | — | 20,648.0 | — |
| `cSumPrices` | `size=512` | 192,700.7 ± 3,170.0 | — | 1,240,694.8 | — |
| `cSumPrices` | `size=64` | 24,975.2 ± 1,564.5 | — | 157,739.1 | — |
| `cSumPrices` | `size=8` | 3,503.4 ± 167.9 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 104.6 ± 2.0 | — | 0.0 | — |
| `jMiss` | `size=64` | 105.9 ± 2.8 | — | 0.0 | — |
| `jMiss` | `size=8` | 104.5 ± 2.8 | — | 0.0 | — |
| `jModifyId` | `size=512` | 1,763.6 ± 47.8 | — | 41,920.5 | — |
| `jModifyId` | `size=64` | 252.6 ± 9.6 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 65.7 ± 2.6 | — | 984.0 | — |
| `jReadId` | `size=512` | 21.6 ± 1.0 | — | 64.0 | — |
| `jReadId` | `size=64` | 23.2 ± 3.3 | — | 64.0 | — |
| `jReadId` | `size=8` | 21.5 ± 1.1 | — | 64.0 | — |
| `jReadStreet` | `size=512` | 122.8 ± 3.1 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 120.4 ± 4.4 | — | 144.0 | — |
| `jReadStreet` | `size=8` | 120.5 ± 2.6 | — | 144.0 | — |
| `jReplaceId` | `size=512` | 1,792.0 ± 45.7 | — | 41,888.5 | — |
| `jReplaceId` | `size=64` | 250.8 ± 5.7 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 66.8 ± 2.0 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 50,192.3 ± 2,056.6 | — | 63,683.9 | — |
| `jSumPrices` | `size=64` | 6,206.9 ± 199.8 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 788.8 ± 19.5 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 41.4 ± 1.2 | — | 312.0 | — |
| `MapDrillModify` | `-` | 23.9 ± 1.4 | — | 216.0 | — |
| `MapGet` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 31.9 ± 4.9 | — | 200.0 | — |
| `handEnvUse` | `-` | 38.0 ± 5.0 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 20.2 ± 0.5 | — | 216.0 | — |
| `handMapGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 27.3 ± 5.7 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.5 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 2.1 ± 0.1 | 2.3 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 20.1 ± 0.4 | 19.3 ± 0.4 | 152.0 | 176.0 |
| `Replace` | `-` | 1.7 ± 0.0 | 1.8 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 10,699.8 ± 1,198.3 | — | 43,034.8 | — |
| `Fold_powerEach` | `size=256` | 2,221.9 ± 52.6 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 243.6 ± 22.3 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 39.4 ± 0.8 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 33,758.1 ± 1,176.4 | — | 331,096.5 | — |
| `Modify_multiFocus` | `size=256` | 8,081.4 ± 313.5 | — | 76,112.6 | — |
| `Modify_multiFocus` | `size=32` | 1,022.8 ± 33.7 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 117.7 ± 3.4 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 22,308.9 ± 545.7 | — | 115,173.8 | — |
| `Modify_powerEach` | `size=256` | 5,383.9 ± 209.1 | — | 26,080.4 | — |
| `Modify_powerEach` | `size=32` | 706.6 ± 26.1 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 102.0 ± 3.9 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 6,132.6 ± 351.4 | — | 65,577.6 | — |
| `naive_listMap` | `size=256` | 1,442.5 ± 45.8 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 142.9 ± 5.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 18.1 ± 0.5 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 3,181.9 ± 88.0 | — | 16,128.8 | — |
| `naive_sumQty` | `size=256` | 393.2 ± 9.6 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 43.2 ± 1.1 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 4.8 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 37.2 ± 0.8 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 98.2 ± 1.8 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 9.6 ± 0.2 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 15.3 ± 0.7 | — | 224.0 | — |
| `naive_constSum` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 24.1 ± 0.4 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 4.0 ± 0.3 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 7.3 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 91.7 ± 3.0 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 26.6 ± 0.7 | — | 184.0 | — |
| `buildAndUse` | `-` | 596.4 ± 17.7 | — | 2,864.0 | — |
| `reuseUse` | `-` | 564.6 ± 16.4 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 13.7 ± 0.1 | 13.7 ± 0.4 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 34.6 ± 0.7 | 36.1 ± 0.7 | 160.0 | 304.0 |
| `Modify_6` | `-` | 84.9 ± 2.3 | 61.6 ± 2.4 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 9.1 ± 0.5 | 9.3 ± 0.5 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 2.6 ± 0.1 | 1.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 4.0 ± 0.1 | 3.4 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 21,934.6 ± 703.7 | — | 97,405.2 | — |
| `ModifyNames` | `size=64` | 2,946.5 ± 651.3 | — | 13,088.1 | — |
| `ModifyNames` | `size=8` | 362.9 ± 8.0 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 79.8 ± 1.9 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 79.3 ± 1.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 80.0 ± 2.1 | — | 320.0 | — |
| `ReadStreet` | `size=512` | 24.3 ± 0.8 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 23.8 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 24.5 ± 0.7 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 63,006.3 ± 1,309.7 | — | 382,784.4 | — |
| `monocleModifyNames` | `size=64` | 6,260.5 ± 169.6 | — | 39,856.2 | — |
| `monocleModifyNames` | `size=8` | 933.4 ± 20.4 | — | 5,448.0 | — |
| `monocleModifyStreet` | `size=512` | 33,483.5 ± 498.3 | — | 169,069.6 | — |
| `monocleModifyStreet` | `size=64` | 4,219.1 ± 117.3 | — | 20,904.1 | — |
| `monocleModifyStreet` | `size=8` | 532.8 ± 15.0 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 20,294.0 ± 545.7 | — | 69,780.9 | — |
| `monocleReadStreet` | `size=64` | 2,564.3 ± 72.6 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 289.4 ± 3.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 42,851.4 ± 925.0 | — | 226,286.3 | — |
| `naiveModifyNames` | `size=64` | 5,270.9 ± 155.2 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 719.4 ± 16.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 34,098.0 ± 934.4 | — | 169,046.1 | — |
| `naiveModifyStreet` | `size=64` | 4,168.4 ± 111.1 | — | 20,880.1 | — |
| `naiveModifyStreet` | `size=8` | 542.3 ± 14.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 20,464.8 ± 549.9 | — | 69,780.9 | — |
| `naiveReadStreet` | `size=64` | 2,565.8 ± 75.6 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 294.8 ± 9.9 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 120,836.4 ± 3,926.7 | — | 609,832.9 | — |
| `Names` | `size=64` | 15,979.0 ± 512.0 | — | 79,297.7 | — |
| `Names` | `size=8` | 2,151.5 ± 49.5 | — | 10,944.0 | — |
| `NamesIor` | `size=512` | 129,145.0 ± 2,779.4 | — | 679,370.2 | — |
| `NamesIor` | `size=64` | 17,222.9 ± 471.8 | — | 87,529.8 | — |
| `NamesIor` | `size=8` | 2,204.3 ± 152.8 | — | 11,520.0 | — |
| `Street` | `size=512` | 544.8 ± 32.2 | — | 2,720.4 | — |
| `Street` | `size=64` | 549.2 ± 17.7 | — | 2,720.1 | — |
| `Street` | `size=8` | 552.6 ± 27.3 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 536.0 ± 8.5 | — | 2,736.4 | — |
| `StreetIor` | `size=64` | 543.9 ± 15.0 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 552.1 ± 23.3 | — | 2,736.0 | — |
| `directNames` | `size=512` | 125,212.5 ± 3,048.8 | — | 613,933.8 | — |
| `directNames` | `size=64` | 15,741.5 ± 588.9 | — | 77,193.7 | — |
| `directNames` | `size=8` | 2,090.8 ± 36.2 | — | 10,688.0 | — |
| `directStreet` | `size=512` | 550.4 ± 18.9 | — | 2,736.4 | — |
| `directStreet` | `size=64` | 559.2 ± 13.9 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 548.8 ± 12.3 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 122,704.9 ± 2,540.8 | — | 601,593.8 | — |
| `hcursorNames` | `size=64` | 15,580.6 ± 492.3 | — | 77,258.2 | — |
| `hcursorNames` | `size=8` | 2,087.0 ± 71.9 | — | 10,696.0 | — |
| `hcursorStreet` | `size=512` | 584.1 ± 15.8 | — | 3,032.5 | — |
| `hcursorStreet` | `size=64` | 573.4 ± 16.5 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 573.8 ± 19.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 128,624.1 ± 2,989.4 | — | 1,121,699.8 | — |
| `monocleNames` | `size=64` | 14,927.7 ± 428.3 | — | 132,766.7 | — |
| `monocleNames` | `size=8` | 2,157.6 ± 65.9 | — | 19,472.0 | — |
| `monocleStreet` | `size=512` | 99,603.2 ± 2,554.7 | — | 907,983.4 | — |
| `monocleStreet` | `size=64` | 13,164.2 ± 385.1 | — | 113,803.8 | — |
| `monocleStreet` | `size=8` | 1,838.8 ± 64.0 | — | 17,058.7 | — |
| `naiveNames` | `size=512` | 109,444.1 ± 4,393.9 | — | 965,216.1 | — |
| `naiveNames` | `size=64` | 14,068.3 ± 467.3 | — | 120,835.9 | — |
| `naiveNames` | `size=8` | 1,933.0 ± 62.6 | — | 17,850.7 | — |
| `naiveStreet` | `size=512` | 99,909.5 ± 2,335.1 | — | 907,986.7 | — |
| `naiveStreet` | `size=64` | 12,994.6 ± 272.6 | — | 113,790.5 | — |
| `naiveStreet` | `size=8` | 1,864.8 ± 93.6 | — | 17,040.0 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 1,958.5 ± 54.8 | — | 42,025.8 | — |
| `ModifyStreet` | `size=64` | 379.8 ± 16.2 | — | 5,432.0 | — |
| `ModifyStreet` | `size=8` | 179.2 ± 5.4 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 125.9 ± 4.3 | — | 128.1 | — |
| `ReadStreet` | `size=64` | 118.1 ± 5.6 | — | 90.7 | — |
| `ReadStreet` | `size=8` | 116.3 ± 8.5 | — | 72.0 | — |
| `SumPrices` | `size=512` | 49,186.0 ± 1,175.9 | — | 63,712.5 | — |
| `SumPrices` | `size=64` | 6,563.2 ± 274.9 | — | 8,120.8 | — |
| `SumPrices` | `size=8` | 810.3 ± 11.3 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 100,864.8 ± 2,274.7 | — | 333,524.6 | — |
| `monocleModifyStreet` | `size=64` | 12,513.1 ± 178.2 | — | 30,081.3 | — |
| `monocleModifyStreet` | `size=8` | 2,191.0 ± 50.1 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 56,327.6 ± 1,003.1 | — | 193,200.2 | — |
| `monocleReadStreet` | `size=64` | 7,152.6 ± 252.2 | — | 24,704.7 | — |
| `monocleReadStreet` | `size=8` | 1,111.4 ± 29.2 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 235,622.1 ± 6,733.5 | — | 1,190,585.7 | — |
| `monocleSumPrices` | `size=64` | 10,165.4 ± 470.3 | — | 47,377.0 | — |
| `monocleSumPrices` | `size=8` | 1,653.9 ± 67.8 | — | 6,624.0 | — |
| `naiveModifyStreet` | `size=512` | 97,650.3 ± 2,800.5 | — | 333,513.6 | — |
| `naiveModifyStreet` | `size=64` | 12,151.9 ± 221.6 | — | 30,057.2 | — |
| `naiveModifyStreet` | `size=8` | 2,139.6 ± 35.8 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 56,300.3 ± 772.6 | — | 193,200.2 | — |
| `naiveReadStreet` | `size=64` | 6,968.0 ± 114.4 | — | 24,704.7 | — |
| `naiveReadStreet` | `size=8` | 1,105.4 ± 26.5 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 60,462.8 ± 2,132.6 | — | 230,091.8 | — |
| `naiveSumPrices` | `size=64` | 7,589.7 ± 141.3 | — | 29,336.8 | — |
| `naiveSumPrices` | `size=8` | 1,160.0 ± 35.2 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 22,405.8 ± 503.5 | — | 442.8 | — |
| `nativeReadStreet` | `size=64` | 2,847.8 ± 22.8 | — | 424.3 | — |
| `nativeReadStreet` | `size=8` | 516.2 ± 21.4 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 38,315.2 ± 731.6 | — | 86,263.4 | — |
| `nativeSumPrices` | `size=64` | 4,882.0 ± 134.2 | — | 10,920.5 | — |
| `nativeSumPrices` | `size=8` | 729.1 ± 9.6 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 73,861.6 ± 4,439.3 | — | 624,349.8 | — |
| `TransformDeep` | `n=512` | 8,834.2 ± 203.9 | — | 57,360.9 | — |
| `TransformDeep` | `n=64` | 1,064.3 ± 46.4 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 78,540.0 ± 3,201.5 | 97,441.0 ± 1,024.6 | 655,321.2 | 753,686.9 |
| `TransformExpr` | `n=512` | 10,184.5 ± 343.3 | 8,785.6 ± 79.8 | 81,825.0 | 69,584.9 |
| `TransformExpr` | `n=64` | 1,208.6 ± 35.9 | 1,490.6 ± 31.3 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 68,898.0 ± 4,359.1 | — | 786,554.1 | — |
| `UniverseDeep` | `n=512` | 9,133.1 ± 287.1 | — | 98,376.9 | — |
| `UniverseDeep` | `n=64` | 1,127.3 ± 63.4 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 71,128.5 ± 1,851.1 | 2,833,105.4 ± 267,948.0 | 786,363.8 | 4,687,739.8 |
| `UniverseExpr` | `n=512` | 8,792.4 ± 427.6 | 220,036.8 ± 7,212.0 | 98,184.9 | 475,014.5 |
| `UniverseExpr` | `n=64` | 1,033.7 ± 27.1 | 20,484.6 ± 875.1 | 12,168.0 | 45,424.4 |
| `UniverseJson` | `n=4096` | 150,712.2 ± 1,655.1 | 3,008,730.8 ± 130,097.9 | 786,421.7 | 6,489,729.6 |
| `UniverseJson` | `n=512` | 16,563.6 ± 387.7 | 239,752.9 ± 5,131.1 | 98,185.7 | 699,920.5 |
| `UniverseJson` | `n=64` | 1,956.4 ± 37.3 | 22,257.3 ± 569.3 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 23,881.6 ± 995.9 | — | 163,873.4 | — |
| `visitorTransformDeep` | `n=512` | 2,625.4 ± 83.5 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 254.3 ± 4.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 39,955.9 ± 929.4 | — | 360,453.1 | — |
| `visitorTransformExpr` | `n=512` | 4,907.5 ± 110.2 | — | 45,032.5 | — |
| `visitorTransformExpr` | `n=64` | 628.5 ± 24.7 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 39,686.4 ± 894.1 | — | 196,692.9 | — |
| `visitorUniverseDeep` | `n=512` | 5,051.1 ± 155.9 | — | 24,632.5 | — |
| `visitorUniverseDeep` | `n=64` | 488.8 ± 30.7 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 33,011.5 ± 619.9 | — | 196,640.0 | — |
| `visitorUniverseExpr` | `n=512` | 3,840.4 ± 68.9 | — | 24,584.4 | — |
| `visitorUniverseExpr` | `n=64` | 467.6 ± 6.2 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 111,898.4 ± 3,972.5 | — | 527,465.4 | — |
| `visitorUniverseJson` | `n=512` | 12,114.6 ± 192.7 | — | 42,313.2 | — |
| `visitorUniverseJson` | `n=64` | 1,202.5 ± 85.1 | — | 4,424.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 8,208.9 ± 269.7 | — | 41,411.9 | — |
| `Modify_powerEach` | `size=16` | 148.0 ± 4.6 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,070.5 ± 39.1 | — | 10,688.3 | — |
| `Modify_powerEach` | `size=4` | 63.0 ± 2.4 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 31,620.3 ± 804.3 | — | 164,339.5 | — |
| `Modify_powerEach` | `size=64` | 535.8 ± 15.7 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 32,252.1 ± 695.8 | — | 279,409.8 | — |
| `monocle_powerEach` | `size=16` | 488.2 ± 38.1 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 12,564.0 ± 491.6 | — | 107,329.9 | — |
| `monocle_powerEach` | `size=4` | 130.9 ± 2.7 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 110,617.0 ± 3,739.1 | — | 967,737.2 | — |
| `monocle_powerEach` | `size=64` | 1,371.6 ± 63.1 | — | 14,520.0 | — |
| `naive_powerEach` | `size=1024` | 4,148.2 ± 265.9 | — | 28,729.9 | — |
| `naive_powerEach` | `size=16` | 48.9 ± 1.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 825.9 ± 16.8 | — | 7,224.1 | — |
| `naive_powerEach` | `size=4` | 15.4 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 13,258.0 ± 322.1 | — | 114,765.6 | — |
| `naive_powerEach` | `size=64` | 195.4 ± 9.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 42,068.2 ± 1,051.5 | — | 210,551.2 | — |
| `Modify_nested` | `size=16` | 999.7 ± 64.3 | — | 4,792.0 | — |
| `Modify_nested` | `size=256` | 11,531.4 ± 90.6 | — | 53,820.6 | — |
| `Modify_nested` | `size=4` | 375.4 ± 12.3 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 2,934.7 ± 79.9 | — | 14,664.3 | — |
| `monocle_nested` | `size=1024` | 121,811.7 ± 1,800.3 | — | 1,118,623.4 | — |
| `monocle_nested` | `size=16` | 1,526.1 ± 176.5 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 51,459.0 ± 2,474.7 | — | 430,187.1 | — |
| `monocle_nested` | `size=4` | 754.2 ± 97.2 | — | 5,557.3 | — |
| `monocle_nested` | `size=64` | 4,763.4 ± 101.7 | — | 58,912.5 | — |
| `naive_nested` | `size=1024` | 13,800.4 ± 638.7 | — | 115,057.1 | — |
| `naive_nested` | `size=16` | 207.3 ± 11.1 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 3,736.8 ± 120.9 | — | 29,018.3 | — |
| `naive_nested` | `size=4` | 82.1 ± 22.9 | — | 792.0 | — |
| `naive_nested` | `size=64` | 892.5 ± 70.9 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 846.6 ± 50.2 | — | 4,816.0 | — |
| `Modify_sparse` | `size=2048` | 13,808.8 ± 857.9 | — | 104,667.1 | — |
| `Modify_sparse` | `size=32` | 198.9 ± 4.3 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 3,415.0 ± 64.0 | — | 24,784.9 | — |
| `Modify_sparse` | `size=8` | 63.4 ± 0.8 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 2,568.5 ± 84.5 | — | 27,808.1 | — |
| `monocle_sparse` | `size=2048` | 55,492.1 ± 2,271.6 | — | 523,113.7 | — |
| `monocle_sparse` | `size=32` | 826.8 ± 76.5 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 18,133.0 ± 584.3 | — | 166,676.5 | — |
| `monocle_sparse` | `size=8` | 193.2 ± 10.0 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 171.0 ± 7.2 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 3,383.7 ± 52.7 | — | 24,610.7 | — |
| `naive_sparse` | `size=32` | 43.7 ± 1.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 778.8 ± 20.1 | — | 6,176.2 | — |
| `naive_sparse` | `size=8` | 13.5 ± 0.4 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.5 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 1.4 ± 0.0 | 1.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.4 ± 0.0 | 1.5 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.4 ± 0.0 | 1.5 ± 0.1 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 1.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 10.9 ± 0.4 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 19.0 ± 0.7 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.0 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 2.8 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 4.5 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 53,672.9 ± 2,577.0 | — | 589,712.4 | — |
| `Cata` | `-` | 43,583.6 ± 299.2 | — | 197,568.3 | — |
| `Hylo` | `-` | 47,085.8 ± 1,480.9 | — | 295,848.3 | — |
| `drosteAna` | `-` | 24,387.9 ± 1,327.8 | — | 327,632.2 | — |
| `drosteCata` | `-` | 25,093.8 ± 2,041.9 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 27,466.7 ± 770.8 | — | 328,640.2 | — |
| `handAna` | `-` | 10,685.0 ± 155.8 | — | 163,816.1 | — |
| `handCata` | `-` | 7,365.9 ± 92.2 | — | 0.1 | — |
| `handHylo` | `-` | 6,259.4 ± 381.3 | — | 0.0 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.3 ± 0.0 | 1.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 7.0 ± 0.3 | 15.0 ± 0.4 | 72.0 | 168.0 |
| `Modify_6` | `-` | 15.7 ± 0.4 | 30.9 ± 1.1 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 1.7 ± 0.0 | 1.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 3,520.8 ± 189.1 | — | 20,200.6 | — |
| `FoldNested` | `size=64` | 329.1 ± 12.0 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 30.1 ± 2.4 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,800.0 ± 101.4 | 18,250.8 ± 2,057.5 | 12,312.3 | 162,578.9 |
| `FoldPrices` | `size=64` | 188.9 ± 3.2 | 1,487.1 ± 55.0 | 1,560.0 | 15,424.0 |
| `FoldPrices` | `size=8` | 22.5 ± 0.9 | 186.9 ± 7.2 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 5,574.1 ± 304.0 | 18,952.5 ± 345.7 | 36,896.9 | 176,923.0 |
| `Modify` | `size=64` | 772.9 ± 55.7 | 1,286.6 ± 44.6 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 52.4 ± 1.1 | 180.5 ± 5.2 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 6.8 ± 0.2 | — | 0.0 | — |
| `DrillModify` | `-` | 75.5 ± 1.6 | — | 528.0 | — |
| `ServiceGet` | `-` | 2.8 ± 0.1 | — | 0.0 | — |
| `ServiceReplace` | `-` | 58.6 ± 1.1 | — | 456.0 | — |
| `handDrillGet` | `-` | 8.4 ± 0.3 | — | 120.0 | — |
| `handDrillModify` | `-` | 72.3 ± 1.6 | — | 648.0 | — |
| `handServiceGet` | `-` | 8.5 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 64.2 ± 1.4 | — | 576.0 | — |


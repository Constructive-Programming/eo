# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `71924f6b7687ce131514a8ddca82d354ceae72df` · date: `2026-09-18` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.4 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.1 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 7.8 ± 0.2 | 5.3 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 18.2 ± 2.3 | 15.0 ± 0.7 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 81.4 ± 1.0 | — | 720.0 | — |
| `ModifyCountry` | `-` | 363.2 ± 8.4 | — | 3,200.0 | — |
| `ModifyPartner` | `-` | 379.1 ± 6.8 | — | 3,256.0 | — |
| `ReadCountry` | `-` | 113.0 ± 5.9 | — | 520.0 | — |
| `ReadPartner` | `-` | 128.3 ± 4.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 188.2 ± 3.2 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 1,638.2 ± 82.1 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 1,905.6 ± 89.9 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,075.8 ± 41.6 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 966.7 ± 24.2 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,253.1 ± 45.7 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 499.9 ± 20.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 348.1 ± 8.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 212.9 ± 4.3 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 249.0 ± 4.2 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 786.8 ± 17.4 | — | 3,666.7 | — |
| `freshDecodeRecord` | `-` | 777.2 ± 135.3 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,041.0 ± 41.5 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 1,739.6 ± 51.6 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 653.7 ± 12.1 | — | 6,568.0 | — |
| `WideToJson` | `-` | 329.4 ± 6.9 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 906.6 ± 46.7 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 1,827.5 ± 140.1 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 626.4 ± 70.3 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,081.3 ± 37.1 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 115.5 ± 3.7 | — | 880.0 | — |
| `decode_native` | `-` | 10.0 ± 0.8 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 109.6 ± 3.7 | — | 880.0 | — |
| `encode_bridged` | `-` | 125.4 ± 2.2 | — | 1,240.0 | — |
| `encode_native` | `-` | 7.6 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 124.3 ± 3.8 | — | 1,234.7 | — |
| `fieldGet_bridged` | `-` | 56.0 ± 0.6 | — | 432.0 | — |
| `fieldGet_native` | `-` | 58.3 ± 2.1 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 206.1 ± 6.2 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 96.3 ± 1.6 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 13.6 ± 0.6 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 14.6 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 13.1 ± 0.2 | — | 0.0 | — |
| `foldMapDirect` | `-` | 13.1 ± 0.3 | — | 0.0 | — |
| `getCap` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 2.3 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 19.3 ± 0.4 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 21.0 ± 0.9 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 2.9 ± 0.3 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 2.4 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 2.0 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 2.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 8.3 ± 1.1 | — | 184.0 | — |
| `buildLens6` | `-` | 15.2 ± 0.4 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 8.3 ± 0.4 | — | 184.0 | — |
| `reuseLeaf` | `-` | 1.6 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 9.6 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 25.3 ± 0.8 | — | 72.0 | — |
| `reuseLens6` | `-` | 74.9 ± 1.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 34.0 ± 0.5 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 2,268.0 ± 107.6 | 2,362.4 ± 73.3 | 14,080.4 | 14,080.4 |
| `FoldMap` | `size=64` | 210.4 ± 7.8 | 217.2 ± 7.7 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 13.1 ± 0.0 | 13.3 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 1,908.0 ± 31.9 | 1,809.1 ± 37.0 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 184.2 ± 11.6 | 187.2 ± 3.3 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 21.5 ± 0.5 | 22.9 ± 1.0 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.0 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 1.7 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 1.5 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 1.6 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.3 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 1.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 8.7 ± 0.4 | 4.1 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 17.7 ± 0.1 | 13.3 ± 0.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.9 ± 0.1 | 2.0 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 1.5 ± 0.1 | 1.6 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 218,026.9 ± 7,301.0 | — | 1,066,662.0 | — |
| `cModifyId` | `size=64` | 29,410.8 ± 929.7 | — | 136,243.1 | — |
| `cModifyId` | `size=8` | 4,687.0 ± 121.3 | — | 20,712.0 | — |
| `cReadId` | `size=512` | 107,852.7 ± 2,639.6 | — | 797,912.6 | — |
| `cReadId` | `size=64` | 14,224.3 ± 1,362.4 | — | 101,288.4 | — |
| `cReadId` | `size=8` | 2,081.1 ± 38.8 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 104,382.7 ± 1,284.7 | — | 797,914.2 | — |
| `cReadStreet` | `size=64` | 13,943.5 ± 441.9 | — | 101,288.4 | — |
| `cReadStreet` | `size=8` | 2,171.8 ± 131.7 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 226,795.6 ± 7,184.2 | — | 1,066,616.5 | — |
| `cReplaceId` | `size=64` | 27,975.8 ± 573.4 | — | 136,211.5 | — |
| `cReplaceId` | `size=8` | 4,765.6 ± 213.5 | — | 20,656.0 | — |
| `cSumPrices` | `size=512` | 187,268.8 ± 4,715.6 | — | 1,240,693.3 | — |
| `cSumPrices` | `size=64` | 22,990.8 ± 191.6 | — | 157,752.7 | — |
| `cSumPrices` | `size=8` | 3,375.2 ± 314.7 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 100.7 ± 2.6 | — | 0.0 | — |
| `jMiss` | `size=64` | 99.8 ± 0.9 | — | 0.0 | — |
| `jMiss` | `size=8` | 98.6 ± 2.7 | — | 0.0 | — |
| `jModifyId` | `size=512` | 1,739.5 ± 89.7 | — | 41,920.5 | — |
| `jModifyId` | `size=64` | 244.1 ± 14.0 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 64.8 ± 1.0 | — | 992.0 | — |
| `jReadId` | `size=512` | 22.1 ± 3.2 | — | 56.0 | — |
| `jReadId` | `size=64` | 21.8 ± 2.1 | — | 48.0 | — |
| `jReadId` | `size=8` | 24.7 ± 4.0 | — | 64.0 | — |
| `jReadStreet` | `size=512` | 114.2 ± 3.3 | — | 144.0 | — |
| `jReadStreet` | `size=64` | 116.6 ± 10.1 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 112.7 ± 1.3 | — | 144.0 | — |
| `jReplaceId` | `size=512` | 1,687.5 ± 37.2 | — | 41,896.5 | — |
| `jReplaceId` | `size=64` | 250.5 ± 20.6 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 64.4 ± 7.8 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 47,624.9 ± 1,244.5 | — | 63,683.0 | — |
| `jSumPrices` | `size=64` | 6,051.9 ± 134.2 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 761.2 ± 24.6 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 36.4 ± 5.1 | — | 312.0 | — |
| `MapDrillModify` | `-` | 21.8 ± 0.5 | — | 216.0 | — |
| `MapGet` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 27.1 ± 2.5 | — | 200.0 | — |
| `handEnvUse` | `-` | 37.5 ± 5.1 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 19.9 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 23.0 ± 0.6 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.5 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 2.0 ± 0.1 | 2.3 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 20.6 ± 0.7 | 18.2 ± 0.8 | 152.0 | 176.0 |
| `Replace` | `-` | 1.6 ± 0.0 | 1.7 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 10,258.5 ± 474.7 | — | 43,034.7 | — |
| `Fold_powerEach` | `size=256` | 2,062.2 ± 24.4 | — | 9,240.1 | — |
| `Fold_powerEach` | `size=32` | 225.1 ± 0.7 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 38.6 ± 3.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 31,368.1 ± 730.6 | — | 331,095.9 | — |
| `Modify_multiFocus` | `size=256` | 7,467.2 ± 83.4 | — | 76,112.5 | — |
| `Modify_multiFocus` | `size=32` | 943.6 ± 26.0 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 115.8 ± 3.3 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 21,407.6 ± 1,003.0 | — | 115,173.6 | — |
| `Modify_powerEach` | `size=256` | 5,043.4 ± 130.2 | — | 26,080.3 | — |
| `Modify_powerEach` | `size=32` | 618.6 ± 10.0 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 95.0 ± 2.1 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 6,034.9 ± 569.8 | — | 65,577.6 | — |
| `naive_listMap` | `size=256` | 1,305.1 ± 65.3 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 130.7 ± 12.2 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 17.9 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 2,977.2 ± 109.9 | — | 16,128.8 | — |
| `naive_sumQty` | `size=256` | 358.0 ± 7.4 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 39.9 ± 1.8 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 4.6 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 34.0 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.0 ± 0.1 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 94.6 ± 2.6 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.9 ± 0.2 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 13.7 ± 0.7 | — | 224.0 | — |
| `naive_constSum` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 22.7 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 3.7 ± 0.2 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 6.9 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 91.4 ± 5.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 26.7 ± 1.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 572.1 ± 16.5 | — | 2,840.0 | — |
| `reuseUse` | `-` | 521.3 ± 14.2 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 12.6 ± 0.1 | 13.6 ± 0.6 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.6 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 33.1 ± 1.3 | 37.0 ± 1.5 | 160.0 | 304.0 |
| `Modify_6` | `-` | 80.2 ± 3.4 | 60.6 ± 2.3 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 8.6 ± 0.0 | 9.2 ± 0.3 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 2.3 ± 0.0 | 1.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 4.1 ± 0.6 | 3.4 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 21,537.7 ± 328.6 | — | 97,405.2 | — |
| `ModifyNames` | `size=64` | 2,645.7 ± 12.2 | — | 12,584.1 | — |
| `ModifyNames` | `size=8` | 357.3 ± 6.3 | — | 2,096.0 | — |
| `ModifyStreet` | `size=512` | 78.8 ± 1.8 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 78.2 ± 1.4 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 78.0 ± 1.0 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 23.5 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 23.5 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 24.1 ± 0.9 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 60,620.9 ± 5,704.7 | — | 382,782.0 | — |
| `monocleModifyNames` | `size=64` | 5,747.5 ± 417.6 | — | 39,840.2 | — |
| `monocleModifyNames` | `size=8` | 869.1 ± 26.9 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 31,178.4 ± 323.0 | — | 169,067.4 | — |
| `monocleModifyStreet` | `size=64` | 3,987.3 ± 56.9 | — | 20,904.1 | — |
| `monocleModifyStreet` | `size=8` | 522.0 ± 15.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 18,876.6 ± 115.3 | — | 69,780.5 | — |
| `monocleReadStreet` | `size=64` | 2,397.4 ± 29.0 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 280.9 ± 9.1 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 40,322.4 ± 509.3 | — | 226,284.2 | — |
| `naiveModifyNames` | `size=64` | 5,072.9 ± 435.2 | — | 27,928.2 | — |
| `naiveModifyNames` | `size=8` | 669.2 ± 17.1 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 31,895.1 ± 1,675.6 | — | 169,043.5 | — |
| `naiveModifyStreet` | `size=64` | 3,986.3 ± 99.6 | — | 20,880.1 | — |
| `naiveModifyStreet` | `size=8` | 514.5 ± 18.9 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 18,836.0 ± 126.1 | — | 69,780.5 | — |
| `naiveReadStreet` | `size=64` | 2,409.2 ± 46.6 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 277.7 ± 7.1 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 123,225.8 ± 5,553.6 | — | 609,834.9 | — |
| `Names` | `size=64` | 16,013.7 ± 143.7 | — | 78,257.7 | — |
| `Names` | `size=8` | 2,134.6 ± 51.0 | — | 10,944.0 | — |
| `NamesIor` | `size=512` | 121,926.4 ± 2,965.9 | — | 675,235.9 | — |
| `NamesIor` | `size=64` | 16,496.4 ± 1,811.5 | — | 86,489.7 | — |
| `NamesIor` | `size=8` | 2,188.7 ± 129.0 | — | 11,520.0 | — |
| `Street` | `size=512` | 527.3 ± 14.0 | — | 2,720.4 | — |
| `Street` | `size=64` | 521.7 ± 12.4 | — | 2,720.1 | — |
| `Street` | `size=8` | 526.2 ± 22.6 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 535.6 ± 31.6 | — | 2,736.4 | — |
| `StreetIor` | `size=64` | 562.2 ± 65.8 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 541.0 ± 20.6 | — | 2,736.0 | — |
| `directNames` | `size=512` | 115,968.0 ± 3,364.6 | — | 609,800.2 | — |
| `directNames` | `size=64` | 15,564.1 ± 1,603.7 | — | 77,193.7 | — |
| `directNames` | `size=8` | 2,030.0 ± 53.2 | — | 10,688.0 | — |
| `directStreet` | `size=512` | 540.3 ± 30.2 | — | 2,736.4 | — |
| `directStreet` | `size=64` | 537.9 ± 17.9 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 537.0 ± 11.0 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 120,283.7 ± 3,943.8 | — | 613,910.8 | — |
| `hcursorNames` | `size=64` | 15,659.4 ± 1,869.4 | — | 77,777.6 | — |
| `hcursorNames` | `size=8` | 2,081.5 ± 101.0 | — | 10,768.0 | — |
| `hcursorStreet` | `size=512` | 569.2 ± 35.1 | — | 3,032.4 | — |
| `hcursorStreet` | `size=64` | 584.5 ± 32.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 547.9 ± 9.0 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 127,983.3 ± 4,047.4 | — | 1,121,699.3 | — |
| `monocleNames` | `size=64` | 14,236.9 ± 212.0 | — | 132,089.3 | — |
| `monocleNames` | `size=8` | 2,153.9 ± 42.8 | — | 19,504.0 | — |
| `monocleStreet` | `size=512` | 97,503.9 ± 1,188.2 | — | 907,983.3 | — |
| `monocleStreet` | `size=64` | 12,897.5 ± 298.4 | — | 113,809.2 | — |
| `monocleStreet` | `size=8` | 1,838.6 ± 25.8 | — | 17,048.0 | — |
| `naiveNames` | `size=512` | 105,153.1 ± 2,721.6 | — | 965,220.1 | — |
| `naiveNames` | `size=64` | 13,563.1 ± 141.6 | — | 120,857.2 | — |
| `naiveNames` | `size=8` | 1,907.3 ± 45.7 | — | 17,808.0 | — |
| `naiveStreet` | `size=512` | 99,103.2 ± 5,433.3 | — | 907,981.6 | — |
| `naiveStreet` | `size=64` | 12,254.6 ± 306.1 | — | 113,801.1 | — |
| `naiveStreet` | `size=8` | 1,788.3 ± 126.1 | — | 17,040.0 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 1,888.2 ± 17.7 | — | 42,025.7 | — |
| `ModifyStreet` | `size=64` | 359.7 ± 8.2 | — | 5,440.0 | — |
| `ModifyStreet` | `size=8` | 169.0 ± 4.1 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 127.3 ± 6.3 | — | 128.1 | — |
| `ReadStreet` | `size=64` | 120.8 ± 4.4 | — | 72.0 | — |
| `ReadStreet` | `size=8` | 119.0 ± 1.4 | — | 72.0 | — |
| `SumPrices` | `size=512` | 49,457.9 ± 1,921.3 | — | 63,712.4 | — |
| `SumPrices` | `size=64` | 6,712.0 ± 355.4 | — | 8,120.8 | — |
| `SumPrices` | `size=8` | 855.0 ± 17.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 100,528.4 ± 3,328.5 | — | 333,523.9 | — |
| `monocleModifyStreet` | `size=64` | 12,355.8 ± 140.9 | — | 30,081.2 | — |
| `monocleModifyStreet` | `size=8` | 2,194.6 ± 48.0 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 57,920.7 ± 1,587.1 | — | 193,201.6 | — |
| `monocleReadStreet` | `size=64` | 6,877.8 ± 132.2 | — | 24,704.7 | — |
| `monocleReadStreet` | `size=8` | 1,084.9 ± 130.8 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 233,428.3 ± 3,900.1 | — | 1,190,583.8 | — |
| `monocleSumPrices` | `size=64` | 9,869.5 ± 111.1 | — | 47,393.0 | — |
| `monocleSumPrices` | `size=8` | 1,700.6 ± 54.6 | — | 6,648.0 | — |
| `naiveModifyStreet` | `size=512` | 100,873.5 ± 3,603.2 | — | 333,517.1 | — |
| `naiveModifyStreet` | `size=64` | 12,249.3 ± 209.7 | — | 30,057.2 | — |
| `naiveModifyStreet` | `size=8` | 2,184.5 ± 90.7 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 56,540.5 ± 1,050.1 | — | 193,200.4 | — |
| `naiveReadStreet` | `size=64` | 7,180.3 ± 181.9 | — | 24,704.7 | — |
| `naiveReadStreet` | `size=8` | 1,115.9 ± 12.7 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 56,562.1 ± 712.1 | — | 230,088.4 | — |
| `naiveSumPrices` | `size=64` | 7,463.5 ± 399.0 | — | 29,336.8 | — |
| `naiveSumPrices` | `size=8` | 1,149.4 ± 23.4 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 22,524.6 ± 672.9 | — | 442.7 | — |
| `nativeReadStreet` | `size=64` | 2,958.0 ± 18.1 | — | 424.3 | — |
| `nativeReadStreet` | `size=8` | 500.1 ± 29.0 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 39,112.8 ± 5,226.2 | — | 86,264.0 | — |
| `nativeSumPrices` | `size=64` | 4,790.5 ± 593.1 | — | 10,920.5 | — |
| `nativeSumPrices` | `size=8` | 688.7 ± 11.0 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 68,057.5 ± 1,041.3 | — | 624,345.5 | — |
| `TransformDeep` | `n=512` | 8,407.8 ± 140.5 | — | 57,360.9 | — |
| `TransformDeep` | `n=64` | 1,013.9 ± 56.8 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 77,914.4 ± 6,856.3 | 92,563.6 ± 3,446.9 | 655,320.7 | 753,683.4 |
| `TransformExpr` | `n=512` | 9,368.7 ± 138.9 | 7,565.9 ± 1,251.0 | 81,825.0 | 66,856.8 |
| `TransformExpr` | `n=64` | 1,129.3 ± 24.0 | 1,539.8 ± 133.5 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 65,719.8 ± 1,243.8 | — | 786,551.8 | — |
| `UniverseDeep` | `n=512` | 8,384.2 ± 140.2 | — | 98,376.9 | — |
| `UniverseDeep` | `n=64` | 1,027.2 ± 27.5 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 66,287.8 ± 1,701.9 | 2,665,069.1 ± 169,722.4 | 786,360.3 | 4,687,617.5 |
| `UniverseExpr` | `n=512` | 8,221.3 ± 71.3 | 207,396.1 ± 3,911.0 | 98,184.8 | 475,013.2 |
| `UniverseExpr` | `n=64` | 974.8 ± 12.9 | 19,012.4 ± 1,109.5 | 12,168.0 | 45,424.4 |
| `UniverseJson` | `n=4096` | 152,584.3 ± 3,489.6 | 2,868,799.7 ± 134,854.2 | 786,423.1 | 6,489,629.6 |
| `UniverseJson` | `n=512` | 16,854.3 ± 531.2 | 228,967.8 ± 4,259.5 | 98,185.7 | 699,919.4 |
| `UniverseJson` | `n=64` | 1,865.1 ± 88.8 | 21,263.6 ± 435.8 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 21,850.6 ± 485.4 | — | 163,871.9 | — |
| `visitorTransformDeep` | `n=512` | 2,509.5 ± 188.3 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 240.8 ± 11.4 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 39,699.2 ± 1,238.9 | — | 360,452.9 | — |
| `visitorTransformExpr` | `n=512` | 4,751.4 ± 240.0 | — | 45,032.5 | — |
| `visitorTransformExpr` | `n=64` | 595.0 ± 8.2 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 37,344.7 ± 1,916.1 | — | 196,691.2 | — |
| `visitorUniverseDeep` | `n=512` | 4,959.0 ± 148.1 | — | 24,632.5 | — |
| `visitorUniverseDeep` | `n=64` | 480.1 ± 6.8 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 33,636.3 ± 816.9 | — | 196,640.5 | — |
| `visitorUniverseExpr` | `n=512` | 3,846.4 ± 39.5 | — | 24,584.4 | — |
| `visitorUniverseExpr` | `n=64` | 448.1 ± 14.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 112,133.8 ± 14,344.2 | — | 449,243.4 | — |
| `visitorUniverseJson` | `n=512` | 12,516.4 ± 260.9 | — | 40,953.3 | — |
| `visitorUniverseJson` | `n=64` | 1,174.0 ± 51.0 | — | 5,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 8,448.2 ± 719.3 | — | 41,412.0 | — |
| `Modify_powerEach` | `size=16` | 143.2 ± 2.7 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,183.2 ± 176.1 | — | 10,688.3 | — |
| `Modify_powerEach` | `size=4` | 62.3 ± 1.2 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 30,981.4 ± 844.1 | — | 164,338.5 | — |
| `Modify_powerEach` | `size=64` | 526.4 ± 28.7 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 31,604.0 ± 2,694.4 | — | 279,409.3 | — |
| `monocle_powerEach` | `size=16` | 433.4 ± 32.6 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 12,506.4 ± 1,076.5 | — | 107,329.9 | — |
| `monocle_powerEach` | `size=4` | 130.5 ± 3.1 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 105,333.8 ± 4,033.8 | — | 967,730.0 | — |
| `monocle_powerEach` | `size=64` | 1,342.1 ± 45.3 | — | 14,520.0 | — |
| `naive_powerEach` | `size=1024` | 3,444.4 ± 257.0 | — | 28,729.6 | — |
| `naive_powerEach` | `size=16` | 46.0 ± 0.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 753.9 ± 12.8 | — | 7,224.1 | — |
| `naive_powerEach` | `size=4` | 14.8 ± 0.9 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 12,479.6 ± 194.2 | — | 114,764.5 | — |
| `naive_powerEach` | `size=64` | 175.8 ± 4.1 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 39,488.9 ± 1,456.9 | — | 210,631.9 | — |
| `Modify_nested` | `size=16` | 905.1 ± 28.1 | — | 4,768.0 | — |
| `Modify_nested` | `size=256` | 10,831.7 ± 821.9 | — | 53,782.8 | — |
| `Modify_nested` | `size=4` | 377.3 ± 12.4 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 2,731.3 ± 101.4 | — | 14,592.3 | — |
| `monocle_nested` | `size=1024` | 126,712.1 ± 11,541.5 | — | 1,118,631.7 | — |
| `monocle_nested` | `size=16` | 1,573.8 ± 131.2 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 52,607.1 ± 3,666.3 | — | 430,188.3 | — |
| `monocle_nested` | `size=4` | 774.1 ± 47.7 | — | 5,557.3 | — |
| `monocle_nested` | `size=64` | 4,798.8 ± 182.7 | — | 58,912.5 | — |
| `naive_nested` | `size=1024` | 13,458.7 ± 466.9 | — | 115,056.5 | — |
| `naive_nested` | `size=16` | 205.5 ± 11.2 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 3,567.3 ± 112.7 | — | 29,018.2 | — |
| `naive_nested` | `size=4` | 73.7 ± 6.0 | — | 792.0 | — |
| `naive_nested` | `size=64` | 821.7 ± 30.2 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 822.1 ± 17.4 | — | 4,816.0 | — |
| `Modify_sparse` | `size=2048` | 13,482.4 ± 305.0 | — | 104,667.0 | — |
| `Modify_sparse` | `size=32` | 194.7 ± 1.7 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 3,447.3 ± 178.5 | — | 24,784.9 | — |
| `Modify_sparse` | `size=8` | 63.2 ± 1.4 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 2,473.2 ± 152.9 | — | 27,808.1 | — |
| `monocle_sparse` | `size=2048` | 55,479.4 ± 941.7 | — | 523,113.7 | — |
| `monocle_sparse` | `size=32` | 769.7 ± 89.5 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 17,767.8 ± 216.4 | — | 166,676.4 | — |
| `monocle_sparse` | `size=8` | 188.4 ± 6.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 167.5 ± 1.4 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 3,290.2 ± 94.7 | — | 24,610.9 | — |
| `naive_sparse` | `size=32` | 44.7 ± 1.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 790.2 ± 85.8 | — | 6,176.2 | — |
| `naive_sparse` | `size=8` | 13.7 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.5 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.3 ± 0.1 | 1.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.4 ± 0.1 | 1.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 1.2 ± 0.1 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 10.1 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 17.7 ± 0.4 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.0 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 2.6 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 4.4 ± 0.4 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 51,547.5 ± 1,242.2 | — | 589,712.4 | — |
| `Cata` | `-` | 43,118.4 ± 3,434.4 | — | 197,568.3 | — |
| `Hylo` | `-` | 45,301.4 ± 1,163.6 | — | 295,848.3 | — |
| `drosteAna` | `-` | 23,351.8 ± 625.7 | — | 327,632.2 | — |
| `drosteCata` | `-` | 23,348.0 ± 1,149.8 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 26,691.1 ± 771.9 | — | 328,640.2 | — |
| `handAna` | `-` | 10,853.2 ± 869.0 | — | 163,816.1 | — |
| `handCata` | `-` | 7,257.5 ± 362.8 | — | 0.1 | — |
| `handHylo` | `-` | 5,993.8 ± 199.4 | — | 0.0 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.3 ± 0.0 | 1.3 ± 0.1 | 24.0 | 24.0 |
| `Modify_3` | `-` | 6.8 ± 0.3 | 14.1 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 15.1 ± 0.5 | 29.6 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 1.7 ± 0.0 | 1.6 ± 0.2 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 3,384.5 ± 326.3 | — | 20,200.5 | — |
| `FoldNested` | `size=64` | 309.9 ± 9.9 | — | 2,704.0 | — |
| `FoldNested` | `size=8` | 28.5 ± 2.6 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,767.6 ± 51.5 | 17,343.4 ± 1,756.5 | 12,312.3 | 162,578.8 |
| `FoldPrices` | `size=64` | 184.2 ± 12.0 | 1,361.3 ± 25.3 | 1,560.0 | 15,424.0 |
| `FoldPrices` | `size=8` | 21.9 ± 0.3 | 180.6 ± 9.5 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 5,183.9 ± 210.7 | 18,444.4 ± 201.6 | 36,896.8 | 176,922.9 |
| `Modify` | `size=64` | 700.0 ± 39.1 | 1,257.0 ± 127.9 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 53.1 ± 0.6 | 174.2 ± 4.0 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 6.5 ± 0.4 | — | 0.0 | — |
| `DrillModify` | `-` | 75.3 ± 3.6 | — | 528.0 | — |
| `ServiceGet` | `-` | 2.7 ± 0.1 | — | 0.0 | — |
| `ServiceReplace` | `-` | 58.1 ± 1.0 | — | 456.0 | — |
| `handDrillGet` | `-` | 8.0 ± 0.5 | — | 120.0 | — |
| `handDrillModify` | `-` | 67.7 ± 0.6 | — | 648.0 | — |
| `handServiceGet` | `-` | 8.1 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 60.0 ± 0.5 | — | 576.0 | — |


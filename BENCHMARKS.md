# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `ef765a2084aa42c61091e7b5cc62e0fc35f274c1` · date: `2026-09-19` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.7 ± 0.0 | 10.1 ± 0.4 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 32.5 ± 0.2 | 24.8 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 168.5 ± 7.6 | — | 720.0 | — |
| `ModifyCountry` | `-` | 422.7 ± 28.8 | — | 3,234.7 | — |
| `ModifyPartner` | `-` | 522.6 ± 18.5 | — | 3,330.7 | — |
| `ReadCountry` | `-` | 185.1 ± 6.6 | — | 520.0 | — |
| `ReadPartner` | `-` | 219.3 ± 5.8 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 335.5 ± 6.4 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,697.0 ± 25.1 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,986.6 ± 28.0 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 5,003.5 ± 23.4 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,738.0 ± 19.2 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,092.4 ± 61.2 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 970.7 ± 9.5 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 645.5 ± 6.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 413.1 ± 4.4 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 434.5 ± 1.8 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,411.3 ± 9.0 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,377.2 ± 8.9 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,181.2 ± 8.4 | — | 9,368.0 | — |
| `ClickToJson` | `-` | 2,898.9 ± 25.6 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 1,040.0 ± 36.6 | — | 6,616.0 | — |
| `WideToJson` | `-` | 710.3 ± 21.0 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,517.5 ± 7.7 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 3,009.8 ± 27.1 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 1,006.4 ± 10.6 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,931.5 ± 29.5 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 221.7 ± 2.8 | — | 880.0 | — |
| `decode_native` | `-` | 18.6 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 215.8 ± 2.6 | — | 880.0 | — |
| `encode_bridged` | `-` | 253.6 ± 13.3 | — | 1,229.3 | — |
| `encode_native` | `-` | 13.6 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 251.0 ± 6.6 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 105.3 ± 1.4 | — | 432.0 | — |
| `fieldGet_native` | `-` | 105.9 ± 0.9 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 435.0 ± 7.7 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 179.6 ± 4.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 22.3 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.8 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.2 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.5 ± 0.7 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.1 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.5 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.9 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.4 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 20.4 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 36.9 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 18.8 ± 0.3 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.9 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 131.8 ± 2.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 58.5 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,859.3 ± 115.6 | 3,944.2 ± 69.6 | 14,080.6 | 14,080.6 |
| `FoldMap` | `size=64` | 372.1 ± 0.9 | 343.0 ± 1.0 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 22.2 ± 1.6 | 22.6 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,785.8 ± 11.9 | 2,785.4 ± 6.2 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 350.5 ± 0.5 | 353.2 ± 1.2 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.1 ± 0.1 | 44.4 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.3 ± 0.1 | 8.7 ± 0.5 | 0.0 | 0.0 |
| `Get_6` | `-` | 35.1 ± 0.1 | 25.1 ± 0.2 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.6 ± 0.0 | 3.8 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 418,087.9 ± 5,075.7 | — | 1,066,724.9 | — |
| `cModifyId` | `size=64` | 53,002.9 ± 274.5 | — | 136,269.9 | — |
| `cModifyId` | `size=8` | 8,897.8 ± 82.8 | — | 20,704.1 | — |
| `cReadId` | `size=512` | 208,016.4 ± 1,230.1 | — | 797,931.2 | — |
| `cReadId` | `size=64` | 26,572.5 ± 382.4 | — | 101,293.8 | — |
| `cReadId` | `size=8` | 4,152.0 ± 71.6 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 219,231.8 ± 17,703.8 | — | 797,934.3 | — |
| `cReadStreet` | `size=64` | 26,715.3 ± 350.7 | — | 101,294.2 | — |
| `cReadStreet` | `size=8` | 4,193.5 ± 41.5 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 418,186.5 ± 3,810.9 | — | 1,066,677.6 | — |
| `cReplaceId` | `size=64` | 53,441.3 ± 1,046.0 | — | 136,221.9 | — |
| `cReplaceId` | `size=8` | 8,747.5 ± 90.8 | — | 20,648.1 | — |
| `cSumPrices` | `size=512` | 352,253.7 ± 1,515.8 | — | 1,240,742.7 | — |
| `cSumPrices` | `size=64` | 43,840.1 ± 456.6 | — | 157,005.4 | — |
| `cSumPrices` | `size=8` | 6,388.1 ± 126.5 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 179.7 ± 5.5 | — | 0.1 | — |
| `jMiss` | `size=64` | 172.9 ± 0.6 | — | 0.0 | — |
| `jMiss` | `size=8` | 177.3 ± 5.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,711.9 ± 80.3 | — | 41,921.1 | — |
| `jModifyId` | `size=64` | 490.5 ± 2.5 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 113.3 ± 1.2 | — | 984.0 | — |
| `jReadId` | `size=512` | 37.1 ± 1.5 | — | 56.0 | — |
| `jReadId` | `size=64` | 39.7 ± 4.0 | — | 48.0 | — |
| `jReadId` | `size=8` | 37.0 ± 1.6 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 197.1 ± 6.6 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 199.9 ± 4.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 199.9 ± 8.5 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 3,711.6 ± 49.7 | — | 41,889.1 | — |
| `jReplaceId` | `size=64` | 477.2 ± 4.9 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 107.3 ± 4.2 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 85,942.9 ± 985.5 | — | 63,663.7 | — |
| `jSumPrices` | `size=64` | 10,896.2 ± 308.3 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,442.5 ± 12.6 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 63.4 ± 2.9 | — | 312.0 | — |
| `MapDrillModify` | `-` | 40.7 ± 0.3 | — | 216.0 | — |
| `MapGet` | `-` | 2.8 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 56.6 ± 1.5 | — | 200.0 | — |
| `handEnvUse` | `-` | 62.9 ± 3.9 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 35.8 ± 0.1 | — | 216.0 | — |
| `handMapGet` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 52.7 ± 3.4 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.9 ± 0.0 | 4.4 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.1 ± 0.3 | 31.1 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.4 ± 0.1 | 3.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 15,304.9 ± 1,386.9 | — | 43,036.0 | — |
| `Fold_powerEach` | `size=256` | 3,471.5 ± 89.4 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 398.6 ± 2.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 87.1 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 52,216.2 ± 1,149.9 | — | 331,116.0 | — |
| `Modify_multiFocus` | `size=256` | 11,648.4 ± 63.9 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,406.3 ± 11.9 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 232.3 ± 6.9 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,746.4 ± 596.3 | — | 115,177.3 | — |
| `Modify_powerEach` | `size=256` | 8,519.5 ± 41.9 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,048.6 ± 3.5 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 198.7 ± 4.3 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,248.4 ± 29.0 | — | 65,578.1 | — |
| `naive_listMap` | `size=256` | 2,082.1 ± 9.9 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 251.8 ± 1.8 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.7 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,412.8 ± 48.9 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 793.4 ± 29.6 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 66.5 ± 1.2 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 7.5 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.8 ± 0.2 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.1 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 173.1 ± 0.5 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 17.2 ± 0.3 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 29.0 ± 0.2 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 36.4 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.9 ± 0.2 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.4 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 167.6 ± 1.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.2 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,134.6 ± 10.9 | — | 2,840.0 | — |
| `reuseUse` | `-` | 1,084.9 ± 41.7 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.8 ± 0.1 | 21.7 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 58.2 ± 0.1 | 74.1 ± 1.5 | 160.0 | 304.0 |
| `Modify_6` | `-` | 143.6 ± 0.5 | 127.1 ± 8.4 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.5 ± 0.1 | 16.8 ± 0.0 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.7 ± 0.0 | 3.8 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.7 ± 0.0 | 7.4 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 36,197.5 ± 1,597.8 | — | 101,504.5 | — |
| `ModifyNames` | `size=64` | 4,277.6 ± 88.1 | — | 13,088.1 | — |
| `ModifyNames` | `size=8` | 596.3 ± 12.1 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 122.3 ± 0.4 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 122.2 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 122.4 ± 0.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 39.2 ± 1.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 38.4 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 38.4 ± 0.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,582.0 ± 2,001.1 | — | 382,769.3 | — |
| `monocleModifyNames` | `size=64` | 9,646.7 ± 525.4 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,476.6 ± 5.8 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 56,469.3 ± 310.0 | — | 169,083.4 | — |
| `monocleModifyStreet` | `size=64` | 7,398.5 ± 100.8 | — | 20,904.3 | — |
| `monocleModifyStreet` | `size=8` | 1,013.6 ± 36.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 34,439.2 ± 215.5 | — | 69,791.8 | — |
| `monocleReadStreet` | `size=64` | 4,428.0 ± 11.1 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 514.6 ± 0.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 70,002.2 ± 267.1 | — | 226,300.9 | — |
| `naiveModifyNames` | `size=64` | 8,956.4 ± 22.0 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,169.0 ± 9.3 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 56,395.6 ± 507.8 | — | 169,062.4 | — |
| `naiveModifyStreet` | `size=64` | 6,840.3 ± 401.6 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 1,001.5 ± 10.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 34,593.8 ± 364.7 | — | 69,791.6 | — |
| `naiveReadStreet` | `size=64` | 4,418.7 ± 16.3 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 514.4 ± 1.4 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 267,858.3 ± 4,365.5 | — | 609,950.7 | — |
| `Names` | `size=64` | 33,151.2 ± 879.0 | — | 79,299.5 | — |
| `Names` | `size=8` | 4,473.6 ± 35.2 | — | 10,728.1 | — |
| `NamesIor` | `size=512` | 270,544.0 ± 2,500.5 | — | 683,560.8 | — |
| `NamesIor` | `size=64` | 35,112.0 ± 661.0 | — | 88,336.6 | — |
| `NamesIor` | `size=8` | 4,860.0 ± 74.4 | — | 11,645.4 | — |
| `Street` | `size=512` | 1,083.4 ± 20.3 | — | 2,720.9 | — |
| `Street` | `size=64` | 1,085.3 ± 20.1 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,089.2 ± 34.7 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,065.7 ± 6.2 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,110.2 ± 9.2 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,121.4 ± 5.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 257,746.0 ± 6,734.6 | — | 609,901.4 | — |
| `directNames` | `size=64` | 31,326.8 ± 837.8 | — | 77,720.6 | — |
| `directNames` | `size=8` | 4,364.2 ± 25.0 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,091.5 ± 20.4 | — | 2,728.9 | — |
| `directStreet` | `size=64` | 1,100.4 ± 47.4 | — | 2,757.4 | — |
| `directStreet` | `size=8` | 1,111.5 ± 37.0 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 252,061.2 ± 3,274.9 | — | 609,896.8 | — |
| `hcursorNames` | `size=64` | 30,487.9 ± 657.8 | — | 77,791.4 | — |
| `hcursorNames` | `size=8` | 4,301.7 ± 54.1 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,166.3 ± 7.5 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,168.1 ± 26.5 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,177.9 ± 24.6 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 234,581.4 ± 2,157.1 | — | 1,121,765.6 | — |
| `monocleNames` | `size=64` | 25,333.6 ± 202.5 | — | 132,771.6 | — |
| `monocleNames` | `size=8` | 3,910.6 ± 88.3 | — | 19,477.4 | — |
| `monocleStreet` | `size=512` | 186,240.1 ± 889.2 | — | 908,029.1 | — |
| `monocleStreet` | `size=64` | 22,461.6 ± 323.1 | — | 113,826.0 | — |
| `monocleStreet` | `size=8` | 3,329.6 ± 25.5 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 208,311.1 ± 7,577.6 | — | 965,276.0 | — |
| `naiveNames` | `size=64` | 24,047.9 ± 99.4 | — | 120,840.6 | — |
| `naiveNames` | `size=8` | 3,542.1 ± 8.2 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 191,149.4 ± 3,288.7 | — | 908,032.4 | — |
| `naiveStreet` | `size=64` | 21,999.3 ± 444.5 | — | 113,426.0 | — |
| `naiveStreet` | `size=8` | 3,372.4 ± 62.4 | — | 17,034.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,048.7 ± 33.5 | — | 42,027.6 | — |
| `ModifyStreet` | `size=64` | 668.8 ± 5.4 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 306.2 ± 10.3 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 215.2 ± 28.3 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 198.2 ± 5.3 | — | 109.4 | — |
| `ReadStreet` | `size=8` | 199.0 ± 2.9 | — | 109.3 | — |
| `SumPrices` | `size=512` | 87,247.6 ± 3,727.0 | — | 63,716.7 | — |
| `SumPrices` | `size=64` | 10,553.3 ± 87.8 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,440.1 ± 8.0 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 165,936.0 ± 1,348.3 | — | 333,578.6 | — |
| `monocleModifyStreet` | `size=64` | 19,952.2 ± 81.0 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,297.4 ± 20.5 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 93,497.7 ± 385.8 | — | 193,232.0 | — |
| `monocleReadStreet` | `size=64` | 11,870.7 ± 118.3 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,890.5 ± 8.8 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 447,253.0 ± 3,510.2 | — | 1,190,768.3 | — |
| `monocleSumPrices` | `size=64` | 16,409.4 ± 82.9 | — | 47,393.7 | — |
| `monocleSumPrices` | `size=8` | 2,579.9 ± 21.9 | — | 6,640.1 | — |
| `naiveModifyStreet` | `size=512` | 165,893.6 ± 846.4 | — | 333,502.0 | — |
| `naiveModifyStreet` | `size=64` | 19,975.7 ± 57.7 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,308.3 ± 18.5 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 93,405.9 ± 365.4 | — | 193,232.0 | — |
| `naiveReadStreet` | `size=64` | 11,861.4 ± 90.7 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,905.0 ± 11.8 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 97,894.9 ± 514.7 | — | 230,123.8 | — |
| `naiveSumPrices` | `size=64` | 12,484.3 ± 135.7 | — | 29,337.3 | — |
| `naiveSumPrices` | `size=8` | 1,992.5 ± 7.0 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 34,584.3 ± 217.2 | — | 452.4 | — |
| `nativeReadStreet` | `size=64` | 4,466.2 ± 27.2 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 829.7 ± 12.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 64,309.7 ± 349.9 | — | 86,274.1 | — |
| `nativeSumPrices` | `size=64` | 7,960.1 ± 20.9 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,293.3 ± 139.9 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 124,192.5 ± 399.8 | — | 624,386.4 | — |
| `TransformDeep` | `n=512` | 11,814.6 ± 75.0 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,454.4 ± 13.8 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 131,095.8 ± 415.8 | 169,707.9 ± 491.6 | 655,359.4 | 753,739.5 |
| `TransformExpr` | `n=512` | 16,429.2 ± 49.9 | 15,557.8 ± 55.3 | 81,825.7 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,043.5 ± 7.6 | 2,644.2 ± 70.3 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 103,713.6 ± 5,869.7 | — | 786,579.5 | — |
| `UniverseDeep` | `n=512` | 15,488.7 ± 103.6 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,914.7 ± 11.2 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 100,212.8 ± 1,075.2 | 2,751,505.7 ± 240,233.0 | 786,384.9 | 4,687,678.5 |
| `UniverseExpr` | `n=512` | 15,098.5 ± 62.1 | 184,104.9 ± 1,497.9 | 98,185.5 | 475,010.8 |
| `UniverseExpr` | `n=64` | 1,839.5 ± 3.8 | 14,917.2 ± 33.9 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 234,554.0 ± 1,088.4 | 2,912,275.6 ± 177,998.1 | 786,482.7 | 6,489,660.3 |
| `UniverseJson` | `n=512` | 27,549.3 ± 82.1 | 206,020.9 ± 2,718.8 | 98,186.8 | 699,917.0 |
| `UniverseJson` | `n=64` | 3,358.5 ± 12.4 | 19,503.1 ± 234.4 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 41,494.1 ± 361.6 | — | 163,886.2 | — |
| `visitorTransformDeep` | `n=512` | 4,900.3 ± 8.8 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 431.4 ± 0.8 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 65,017.3 ± 553.4 | — | 360,471.3 | — |
| `visitorTransformExpr` | `n=512` | 7,603.8 ± 21.0 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 990.4 ± 1.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,264.8 ± 862.4 | — | 196,707.1 | — |
| `visitorUniverseDeep` | `n=512` | 7,250.3 ± 29.3 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 832.9 ± 2.4 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 54,052.2 ± 648.6 | — | 196,655.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,765.3 ± 17.0 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 822.5 ± 1.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 190,666.3 ± 16,298.7 | — | 458,866.8 | — |
| `visitorUniverseJson` | `n=512` | 21,232.0 ± 155.8 | — | 40,954.2 | — |
| `visitorUniverseJson` | `n=64` | 2,005.3 ± 28.2 | — | 5,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,867.3 ± 156.6 | — | 41,414.1 | — |
| `Modify_powerEach` | `size=16` | 266.4 ± 4.7 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,108.3 ± 6.6 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 125.1 ± 0.7 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 54,486.7 ± 1,741.6 | — | 164,376.8 | — |
| `Modify_powerEach` | `size=64` | 833.7 ± 2.8 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 59,626.0 ± 1,452.7 | — | 279,432.6 | — |
| `monocle_powerEach` | `size=16` | 639.2 ± 5.8 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,922.3 ± 138.9 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 262.3 ± 7.7 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 184,607.3 ± 3,862.5 | — | 967,852.6 | — |
| `monocle_powerEach` | `size=64` | 2,116.6 ± 6.6 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,210.7 ± 16.7 | — | 28,730.5 | — |
| `naive_powerEach` | `size=16` | 99.5 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,633.9 ± 7.3 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.5 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 20,642.8 ± 157.3 | — | 114,777.8 | — |
| `naive_powerEach` | `size=64` | 399.4 ± 5.5 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 71,985.9 ± 6,029.3 | — | 210,651.0 | — |
| `Modify_nested` | `size=16` | 1,502.3 ± 11.1 | — | 4,720.1 | — |
| `Modify_nested` | `size=256` | 18,469.1 ± 132.8 | — | 53,899.5 | — |
| `Modify_nested` | `size=4` | 701.6 ± 7.8 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,838.0 ± 8.9 | — | 14,592.6 | — |
| `monocle_nested` | `size=1024` | 249,612.0 ± 3,159.3 | — | 1,118,854.4 | — |
| `monocle_nested` | `size=16` | 2,919.2 ± 48.4 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 92,980.7 ± 1,564.3 | — | 430,209.5 | — |
| `monocle_nested` | `size=4` | 1,332.8 ± 82.0 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 8,882.0 ± 138.3 | — | 58,903.1 | — |
| `naive_nested` | `size=1024` | 20,070.5 ± 179.1 | — | 115,068.5 | — |
| `naive_nested` | `size=16` | 372.5 ± 4.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,287.7 ± 27.3 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 129.3 ± 2.6 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,348.2 ± 35.5 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,365.5 ± 7.2 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,293.9 ± 111.5 | — | 104,674.9 | — |
| `Modify_sparse` | `size=32` | 361.6 ± 3.1 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,742.2 ± 38.9 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 121.0 ± 0.5 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,941.3 ± 8.6 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 107,752.3 ± 837.7 | — | 523,147.2 | — |
| `monocle_sparse` | `size=32` | 1,099.6 ± 41.3 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 34,498.7 ± 288.5 | — | 166,686.3 | — |
| `monocle_sparse` | `size=8` | 335.4 ± 1.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 325.1 ± 0.9 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,159.0 ± 69.0 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 84.5 ± 3.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,199.1 ± 9.9 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.3 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.5 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.5 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 20.0 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.6 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.6 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.0 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 106,863.1 ± 1,033.2 | — | 589,712.8 | — |
| `Cata` | `-` | 78,852.6 ± 670.0 | — | 197,568.6 | — |
| `Hylo` | `-` | 85,959.6 ± 581.9 | — | 295,848.6 | — |
| `drosteAna` | `-` | 47,877.4 ± 449.7 | — | 327,632.3 | — |
| `drosteCata` | `-` | 40,106.9 ± 2,030.6 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 65,130.0 ± 149.1 | — | 328,640.5 | — |
| `handAna` | `-` | 32,314.8 ± 21,905.5 | — | 163,816.2 | — |
| `handCata` | `-` | 13,042.5 ± 35.9 | — | 0.1 | — |
| `handHylo` | `-` | 9,704.5 ± 271.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.9 ± 0.0 | 28.4 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.5 ± 0.2 | 65.3 ± 0.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.2 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,230.3 ± 143.9 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 554.2 ± 6.5 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 54.9 ± 3.9 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 2,815.3 ± 7.6 | 31,726.1 ± 166.2 | 12,312.4 | 162,581.0 |
| `FoldPrices` | `size=64` | 352.4 ± 0.6 | 2,137.4 ± 68.3 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 44.1 ± 0.7 | 349.4 ± 4.5 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 7,654.5 ± 169.2 | 33,815.9 ± 301.3 | 36,897.2 | 176,925.2 |
| `Modify` | `size=64` | 870.0 ± 4.3 | 1,759.2 ± 16.0 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 97.5 ± 0.3 | 351.7 ± 93.2 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 12.0 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 115.6 ± 1.1 | — | 528.0 | — |
| `ServiceGet` | `-` | 5.1 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 82.1 ± 0.3 | — | 456.0 | — |
| `handDrillGet` | `-` | 14.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 107.1 ± 0.6 | — | 648.0 | — |
| `handServiceGet` | `-` | 15.0 ± 0.0 | — | 120.0 | — |
| `handServiceReplace` | `-` | 90.0 ± 0.7 | — | 576.0 | — |


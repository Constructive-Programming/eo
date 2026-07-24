# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `9694689b34af678a688622219b2397f7acc9d1e5` · date: `2026-07-24` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.1 ± 0.0 | 10.2 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.8 ± 0.3 | 24.1 ± 0.6 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 160.6 ± 3.8 | — | 720.0 | — |
| `ModifyCountry` | `-` | 329.8 ± 7.7 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 389.0 ± 2.8 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 179.9 ± 2.4 | — | 520.0 | — |
| `ReadPartner` | `-` | 217.9 ± 11.2 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 328.4 ± 1.2 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,836.6 ± 96.9 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,734.9 ± 50.0 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,218.2 ± 19.8 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,706.3 ± 18.1 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,724.9 ± 25.2 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 753.5 ± 3.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 555.6 ± 22.2 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 415.7 ± 3.1 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 472.2 ± 40.3 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,318.8 ± 11.6 | — | 3,677.3 | — |
| `freshDecodeRecord` | `-` | 1,299.2 ± 24.3 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,548.0 ± 74.7 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 3,053.8 ± 83.7 | — | 4,016.0 | — |
| `WideToAvro` | `-` | 804.5 ± 19.1 | — | 6,536.0 | — |
| `WideToJson` | `-` | 604.3 ± 6.7 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,528.0 ± 152.2 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,895.3 ± 85.8 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,002.4 ± 26.7 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,955.5 ± 36.6 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 223.1 ± 2.6 | — | 984.0 | — |
| `decode_native` | `-` | 20.4 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 216.2 ± 1.0 | — | 984.0 | — |
| `encode_bridged` | `-` | 239.2 ± 16.7 | — | 1,272.0 | — |
| `encode_native` | `-` | 15.9 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 236.2 ± 7.6 | — | 1,304.0 | — |
| `fieldGet_bridged` | `-` | 110.1 ± 17.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 98.8 ± 0.9 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 393.1 ± 9.3 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 182.5 ± 3.1 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.9 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.8 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.1 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 33.8 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 38.9 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.5 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.2 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 22.3 ± 0.3 | — | 184.0 | — |
| `buildLens6` | `-` | 43.7 ± 0.6 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 22.5 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.4 ± 0.0 | — | 40.0 | — |
| `reuseLens3` | `-` | 48.9 ± 0.7 | — | 72.0 | — |
| `reuseLens6` | `-` | 137.5 ± 8.5 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 63.6 ± 0.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 5,058.5 ± 56.4 | 4,724.2 ± 11.8 | 14,080.8 | 14,080.8 |
| `FoldMap` | `size=64` | 326.0 ± 3.5 | 313.2 ± 2.8 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.5 ± 0.2 | 21.9 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,121.7 ± 16.0 | 3,131.1 ± 21.3 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 374.2 ± 6.5 | 372.6 ± 1.1 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 48.1 ± 0.2 | 48.2 ± 0.6 | 216.0 | 216.0 |

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
| `handLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.1 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.6 ± 0.1 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.1 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.4 ± 0.1 | 8.8 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 32.7 ± 0.7 | 27.5 ± 0.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.8 ± 0.1 | 4.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.4 ± 0.0 | 3.3 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 434,901.4 ± 3,584.8 | — | 1,072,937.4 | — |
| `cModifyId` | `size=64` | 55,764.2 ± 257.1 | — | 136,333.2 | — |
| `cModifyId` | `size=8` | 9,580.5 ± 261.8 | — | 21,240.1 | — |
| `cReadId` | `size=512` | 225,560.7 ± 8,487.4 | — | 804,112.1 | — |
| `cReadId` | `size=64` | 28,566.3 ± 197.2 | — | 101,325.7 | — |
| `cReadId` | `size=8` | 4,562.7 ± 50.9 | — | 15,608.0 | — |
| `cReadStreet` | `size=512` | 226,270.5 ± 3,020.1 | — | 804,120.3 | — |
| `cReadStreet` | `size=64` | 29,195.2 ± 1,200.9 | — | 101,333.9 | — |
| `cReadStreet` | `size=8` | 4,606.2 ± 17.6 | — | 15,608.0 | — |
| `cReplaceId` | `size=512` | 433,049.6 ± 2,249.4 | — | 1,072,849.6 | — |
| `cReplaceId` | `size=64` | 56,079.9 ± 785.3 | — | 136,254.6 | — |
| `cReplaceId` | `size=8` | 9,825.6 ± 120.1 | — | 21,400.1 | — |
| `cSumPrices` | `size=512` | 363,855.2 ± 13,331.9 | — | 1,250,967.4 | — |
| `cSumPrices` | `size=64` | 44,621.5 ± 126.3 | — | 157,514.7 | — |
| `cSumPrices` | `size=8` | 6,646.9 ± 93.4 | — | 22,784.1 | — |
| `jMiss` | `size=512` | 193.4 ± 1.5 | — | 0.1 | — |
| `jMiss` | `size=64` | 193.1 ± 0.5 | — | 0.0 | — |
| `jMiss` | `size=8` | 192.8 ± 0.3 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,920.1 ± 116.3 | — | 41,936.8 | — |
| `jModifyId` | `size=64` | 329.1 ± 10.0 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 105.5 ± 2.1 | — | 992.0 | — |
| `jReadId` | `size=512` | 36.1 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.8 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=8` | 37.4 ± 2.1 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 210.2 ± 0.7 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 215.7 ± 5.3 | — | 144.0 | — |
| `jReadStreet` | `size=8` | 211.7 ± 3.0 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 2,838.4 ± 18.7 | — | 41,888.8 | — |
| `jReplaceId` | `size=64` | 337.2 ± 12.6 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 98.9 ± 1.8 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 88,541.4 ± 312.8 | — | 63,664.0 | — |
| `jSumPrices` | `size=64` | 11,000.3 ± 125.8 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,458.1 ± 11.9 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.2 | 4.0 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 39.7 ± 0.9 | 32.9 ± 0.5 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.0 | 3.2 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 17,768.7 ± 2,291.9 | — | 43,036.6 | — |
| `Fold_powerEach` | `size=256` | 3,919.5 ± 60.7 | — | 9,240.3 | — |
| `Fold_powerEach` | `size=32` | 490.9 ± 7.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 80.4 ± 1.2 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 52,903.7 ± 633.8 | — | 380,251.8 | — |
| `Modify_multiFocus` | `size=256` | 13,284.0 ± 93.3 | — | 88,422.2 | — |
| `Modify_multiFocus` | `size=32` | 1,538.1 ± 6.5 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 226.9 ± 1.3 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 38,288.6 ± 739.4 | — | 115,201.9 | — |
| `Modify_powerEach` | `size=256` | 9,029.4 ± 121.1 | — | 26,104.6 | — |
| `Modify_powerEach` | `size=32` | 870.3 ± 15.0 | — | 3,224.0 | — |
| `Modify_powerEach` | `size=4` | 179.8 ± 3.2 | — | 872.0 | — |
| `naive_listMap` | `size=1024` | 9,415.7 ± 129.7 | — | 65,578.4 | — |
| `naive_listMap` | `size=256` | 2,282.0 ± 39.9 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 267.2 ± 5.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 35.4 ± 0.4 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 5,159.8 ± 89.7 | — | 16,129.3 | — |
| `naive_sumQty` | `size=256` | 969.7 ± 23.5 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 78.1 ± 0.7 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.7 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 68.7 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 185.0 ± 1.3 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 16.3 ± 0.2 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.1 ± 0.3 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 42.2 ± 0.6 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.0 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.6 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 164.2 ± 3.0 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 49.4 ± 0.6 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,080.8 ± 37.8 | — | 2,816.0 | — |
| `reuseUse` | `-` | 1,017.9 ± 11.8 | — | 2,648.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.9 ± 0.3 | 23.2 ± 0.3 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 63.6 ± 0.2 | 68.2 ± 2.2 | 160.0 | 304.0 |
| `Modify_6` | `-` | 162.5 ± 17.3 | 109.5 ± 0.6 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 21.4 ± 0.5 | 20.8 ± 1.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.2 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.8 ± 0.2 | 7.4 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 40,060.9 ± 1,303.0 | — | 97,430.9 | — |
| `ModifyNames` | `size=64` | 5,011.7 ± 77.3 | — | 12,624.2 | — |
| `ModifyNames` | `size=8` | 684.9 ± 13.8 | — | 2,160.0 | — |
| `ModifyStreet` | `size=512` | 143.6 ± 0.3 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 149.1 ± 9.2 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 144.2 ± 2.6 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 43.4 ± 2.4 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 45.2 ± 4.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 42.9 ± 0.4 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,713.0 ± 1,011.9 | — | 382,771.8 | — |
| `monocleModifyNames` | `size=64` | 10,556.3 ± 601.7 | — | 39,848.4 | — |
| `monocleModifyNames` | `size=8` | 1,502.9 ± 45.2 | — | 5,432.0 | — |
| `monocleModifyStreet` | `size=512` | 58,366.4 ± 206.4 | — | 169,084.8 | — |
| `monocleModifyStreet` | `size=64` | 7,628.9 ± 78.8 | — | 20,904.3 | — |
| `monocleModifyStreet` | `size=8` | 988.4 ± 11.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 36,607.7 ± 537.2 | — | 69,792.6 | — |
| `monocleReadStreet` | `size=64` | 4,682.2 ± 59.6 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 548.0 ± 6.6 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 74,696.3 ± 1,499.8 | — | 226,301.5 | — |
| `naiveModifyNames` | `size=64` | 9,481.9 ± 114.2 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,138.8 ± 27.8 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 58,503.5 ± 241.9 | — | 169,063.5 | — |
| `naiveModifyStreet` | `size=64` | 7,572.3 ± 60.0 | — | 20,880.3 | — |
| `naiveModifyStreet` | `size=8` | 990.4 ± 18.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 36,797.0 ± 699.1 | — | 69,792.8 | — |
| `naiveReadStreet` | `size=64` | 4,687.0 ± 49.1 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 546.0 ± 7.1 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 236,258.1 ± 2,412.3 | — | 614,026.0 | — |
| `Names` | `size=64` | 30,409.6 ± 352.0 | — | 79,299.2 | — |
| `Names` | `size=8` | 4,085.3 ± 49.4 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 252,605.4 ± 10,351.7 | — | 679,438.9 | — |
| `NamesIor` | `size=64` | 32,904.6 ± 627.8 | — | 87,532.7 | — |
| `NamesIor` | `size=8` | 4,247.4 ± 65.1 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,019.6 ± 12.1 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,015.1 ± 9.8 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,021.4 ± 17.6 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,013.9 ± 13.8 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,013.3 ± 5.8 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,013.6 ± 6.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 241,452.0 ± 6,548.4 | — | 609,886.2 | — |
| `directNames` | `size=64` | 30,039.9 ± 581.6 | — | 77,724.6 | — |
| `directNames` | `size=8` | 4,040.6 ± 55.5 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,027.6 ± 27.7 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,043.9 ± 31.9 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,022.6 ± 11.5 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 244,548.9 ± 4,491.2 | — | 613,992.5 | — |
| `hcursorNames` | `size=64` | 30,566.8 ± 1,021.4 | — | 77,788.2 | — |
| `hcursorNames` | `size=8` | 4,095.0 ± 151.9 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,070.3 ± 49.6 | — | 3,008.8 | — |
| `hcursorStreet` | `size=64` | 1,078.9 ± 21.0 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,097.3 ± 27.9 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 236,584.0 ± 7,050.2 | — | 1,121,767.0 | — |
| `monocleNames` | `size=64` | 25,808.8 ± 545.1 | — | 132,758.9 | — |
| `monocleNames` | `size=8` | 3,860.1 ± 83.0 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 191,390.4 ± 9,888.2 | — | 908,040.6 | — |
| `monocleStreet` | `size=64` | 22,312.0 ± 206.6 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,238.4 ± 10.2 | — | 17,058.7 | — |
| `naiveNames` | `size=512` | 205,479.2 ± 3,377.3 | — | 965,274.1 | — |
| `naiveNames` | `size=64` | 24,239.3 ± 254.0 | — | 120,847.5 | — |
| `naiveNames` | `size=8` | 3,454.3 ± 11.2 | — | 17,824.1 | — |
| `naiveStreet` | `size=512` | 188,634.6 ± 6,428.6 | — | 908,036.1 | — |
| `naiveStreet` | `size=64` | 22,268.1 ± 123.3 | — | 113,796.7 | — |
| `naiveStreet` | `size=8` | 3,257.4 ± 30.7 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,132.6 ± 17.9 | — | 42,026.8 | — |
| `ModifyStreet` | `size=64` | 526.8 ± 6.3 | — | 5,440.1 | — |
| `ModifyStreet` | `size=8` | 303.4 ± 3.9 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 211.2 ± 1.0 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 210.0 ± 3.3 | — | 101.4 | — |
| `ReadStreet` | `size=8` | 211.1 ± 0.8 | — | 128.0 | — |
| `SumPrices` | `size=512` | 89,019.0 ± 591.1 | — | 63,720.2 | — |
| `SumPrices` | `size=64` | 10,977.7 ± 173.7 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,484.6 ± 29.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 171,960.2 ± 750.3 | — | 333,615.7 | — |
| `monocleModifyStreet` | `size=64` | 21,628.7 ± 284.2 | — | 30,114.2 | — |
| `monocleModifyStreet` | `size=8` | 3,522.2 ± 11.2 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 98,682.0 ± 597.5 | — | 193,268.5 | — |
| `monocleReadStreet` | `size=64` | 12,517.4 ± 91.0 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,900.8 ± 23.9 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 473,264.8 ± 5,656.9 | — | 1,190,821.3 | — |
| `monocleSumPrices` | `size=64` | 17,000.6 ± 144.0 | — | 47,401.7 | — |
| `monocleSumPrices` | `size=8` | 2,608.8 ± 65.0 | — | 6,680.1 | — |
| `naiveModifyStreet` | `size=512` | 172,422.6 ± 461.0 | — | 333,574.1 | — |
| `naiveModifyStreet` | `size=64` | 21,485.4 ± 86.3 | — | 30,090.2 | — |
| `naiveModifyStreet` | `size=8` | 3,494.8 ± 24.4 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 98,693.6 ± 2,020.0 | — | 193,268.5 | — |
| `naiveReadStreet` | `size=64` | 12,436.2 ± 72.5 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,883.5 ± 3.9 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 103,040.3 ± 257.4 | — | 230,160.2 | — |
| `naiveSumPrices` | `size=64` | 13,073.7 ± 127.4 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 1,962.3 ± 9.9 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 42,654.2 ± 986.8 | — | 458.8 | — |
| `nativeReadStreet` | `size=64` | 5,202.8 ± 225.1 | — | 424.6 | — |
| `nativeReadStreet` | `size=8` | 866.1 ± 7.7 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 67,931.0 ± 742.4 | — | 86,273.2 | — |
| `nativeSumPrices` | `size=64` | 8,474.5 ± 39.8 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,236.3 ± 5.3 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 136,836.9 ± 6,469.2 | — | 624,395.6 | — |
| `TransformDeep` | `n=512` | 13,541.5 ± 130.6 | — | 57,361.4 | — |
| `TransformDeep` | `n=64` | 1,545.7 ± 18.1 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 149,396.2 ± 4,367.8 | 184,236.5 ± 630.2 | 655,372.7 | 753,750.1 |
| `TransformExpr` | `n=512` | 19,017.1 ± 246.0 | 16,605.7 ± 52.8 | 81,826.0 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,364.9 ± 63.7 | 2,830.2 ± 8.1 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 112,489.1 ± 4,878.8 | — | 786,585.9 | — |
| `UniverseDeep` | `n=512` | 16,252.0 ± 59.3 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,974.0 ± 5.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 101,362.4 ± 658.4 | 1,813,216.0 ± 46,874.8 | 786,385.8 | 4,752,534.5 |
| `UniverseExpr` | `n=512` | 15,813.5 ± 145.9 | 179,898.8 ± 6,054.4 | 98,185.6 | 483,202.4 |
| `UniverseExpr` | `n=64` | 1,921.1 ± 4.3 | 16,461.0 ± 203.3 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 243,902.0 ± 4,742.9 | 2,042,889.3 ± 70,581.4 | 786,489.5 | 6,489,029.3 |
| `UniverseJson` | `n=512` | 28,113.5 ± 261.4 | 207,095.4 ± 2,317.0 | 98,186.9 | 699,917.1 |
| `UniverseJson` | `n=64` | 3,376.1 ± 47.1 | 20,608.2 ± 218.2 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 44,805.2 ± 238.4 | — | 163,888.6 | — |
| `visitorTransformDeep` | `n=512` | 4,081.2 ± 65.7 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 449.3 ± 1.3 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 73,623.4 ± 1,353.2 | — | 360,477.6 | — |
| `visitorTransformExpr` | `n=512` | 9,049.5 ± 86.9 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,125.3 ± 13.9 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,272.5 ± 740.9 | — | 196,707.1 | — |
| `visitorUniverseDeep` | `n=512` | 7,324.4 ± 107.7 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 832.0 ± 5.9 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,295.5 ± 187.8 | — | 196,656.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,988.4 ± 230.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 820.8 ± 1.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 127,630.0 ± 1,077.2 | — | 294,988.9 | — |
| `visitorUniverseJson` | `n=512` | 14,381.5 ± 148.4 | — | 36,849.5 | — |
| `visitorUniverseJson` | `n=64` | 1,990.3 ± 15.4 | — | 4,592.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 15,068.2 ± 168.7 | — | 41,439.1 | — |
| `Modify_powerEach` | `size=16` | 301.2 ± 8.8 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,597.6 ± 45.8 | — | 10,712.6 | — |
| `Modify_powerEach` | `size=4` | 135.6 ± 1.6 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 59,046.5 ± 507.1 | — | 164,408.6 | — |
| `Modify_powerEach` | `size=64` | 944.2 ± 10.8 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 58,725.5 ± 1,489.1 | — | 279,431.9 | — |
| `monocle_powerEach` | `size=16` | 593.5 ± 30.1 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,502.3 ± 252.3 | — | 107,339.3 | — |
| `monocle_powerEach` | `size=4` | 237.6 ± 13.5 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 182,435.2 ± 5,470.4 | — | 967,849.1 | — |
| `monocle_powerEach` | `size=64` | 2,105.2 ± 23.6 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,813.3 ± 44.4 | — | 28,730.8 | — |
| `naive_powerEach` | `size=16` | 111.1 ± 1.0 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,733.2 ± 66.7 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 28.7 ± 0.4 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,275.1 ± 408.8 | — | 114,782.1 | — |
| `naive_powerEach` | `size=64` | 425.8 ± 2.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 63,739.5 ± 649.8 | — | 210,612.0 | — |
| `Modify_nested` | `size=16` | 1,598.7 ± 48.0 | — | 4,936.1 | — |
| `Modify_nested` | `size=256` | 19,484.0 ± 298.6 | — | 53,868.2 | — |
| `Modify_nested` | `size=4` | 730.2 ± 5.5 | — | 2,408.0 | — |
| `Modify_nested` | `size=64` | 4,915.2 ± 91.6 | — | 14,760.6 | — |
| `monocle_nested` | `size=1024` | 251,136.4 ± 5,990.4 | — | 1,118,857.2 | — |
| `monocle_nested` | `size=16` | 2,623.0 ± 45.7 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 91,872.7 ± 788.3 | — | 430,210.1 | — |
| `monocle_nested` | `size=4` | 1,202.8 ± 108.2 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,665.8 ± 182.1 | — | 58,912.9 | — |
| `naive_nested` | `size=1024` | 22,378.7 ± 608.2 | — | 115,072.8 | — |
| `naive_nested` | `size=16` | 412.2 ± 7.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,228.1 ± 59.4 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 145.1 ± 4.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,476.1 ± 24.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,618.4 ± 11.4 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 29,862.2 ± 425.7 | — | 104,803.2 | — |
| `Modify_sparse` | `size=32` | 541.3 ± 7.9 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,471.4 ± 37.0 | — | 24,906.1 | — |
| `Modify_sparse` | `size=8` | 175.1 ± 3.1 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,805.3 ± 33.0 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 95,627.1 ± 1,054.6 | — | 474,665.7 | — |
| `monocle_sparse` | `size=32` | 982.2 ± 41.3 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,330.2 ± 299.2 | — | 156,442.5 | — |
| `monocle_sparse` | `size=8` | 276.7 ± 4.6 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 323.1 ± 1.6 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,515.2 ± 25.4 | — | 24,612.6 | — |
| `naive_sparse` | `size=32` | 83.7 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,284.6 ± 3.5 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 26.0 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.7 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.7 ± 0.6 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 38.1 ± 1.5 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.9 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.3 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 122,107.6 ± 2,888.5 | — | 786,296.9 | — |
| `Cata` | `-` | 84,052.0 ± 412.9 | — | 197,568.6 | — |
| `Hylo` | `-` | 88,169.7 ± 1,580.7 | — | 295,848.6 | — |
| `drosteAna` | `-` | 52,210.0 ± 3,117.3 | — | 327,632.4 | — |
| `drosteCata` | `-` | 47,053.4 ± 851.9 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,725.9 ± 2,713.4 | — | 328,640.4 | — |
| `handAna` | `-` | 21,505.4 ± 322.7 | — | 163,816.1 | — |
| `handCata` | `-` | 12,964.7 ± 46.6 | — | 0.1 | — |
| `handHylo` | `-` | 12,455.0 ± 493.4 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.1 | 2.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.2 ± 0.2 | 26.4 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.2 ± 0.2 | 52.8 ± 1.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,446.4 ± 101.4 | — | 20,248.9 | — |
| `FoldNested` | `size=64` | 610.1 ± 5.3 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 61.9 ± 4.8 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 3,083.9 ± 47.4 | 30,619.6 ± 308.1 | 12,312.5 | 162,580.9 |
| `FoldPrices` | `size=64` | 372.6 ± 1.5 | 2,274.5 ± 7.6 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 48.2 ± 0.3 | 279.4 ± 19.4 | 216.0 | 2,010.7 |
| `Modify` | `size=512` | 9,045.3 ± 179.2 | 34,595.2 ± 1,213.6 | 36,897.4 | 176,925.3 |
| `Modify` | `size=64` | 967.6 ± 9.4 | 1,782.7 ± 2.5 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 114.2 ± 1.8 | 242.6 ± 4.4 | 608.0 | 1,936.0 |


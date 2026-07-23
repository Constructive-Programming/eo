# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `688eee7116e1a0884cd6977959aa4ed2654b635e` · date: `2026-07-23` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.2 ± 0.1 | 10.2 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 30.2 ± 0.7 | 24.8 ± 0.8 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 160.9 ± 0.9 | — | 720.0 | — |
| `ModifyCountry` | `-` | 336.6 ± 10.7 | — | 3,189.3 | — |
| `ModifyPartner` | `-` | 393.9 ± 5.0 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 180.1 ± 2.2 | — | 520.0 | — |
| `ReadPartner` | `-` | 219.0 ± 6.4 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 332.0 ± 5.6 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,754.0 ± 15.8 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,744.1 ± 45.3 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,258.5 ± 55.2 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,755.0 ± 64.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,726.6 ± 8.2 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 840.3 ± 130.0 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 546.6 ± 5.0 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 415.9 ± 5.4 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 438.0 ± 5.1 | — | 1,541.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,384.0 ± 68.7 | — | 3,685.3 | — |
| `freshDecodeRecord` | `-` | 1,310.4 ± 35.6 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,543.9 ± 37.5 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 3,028.6 ± 45.3 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 829.5 ± 16.0 | — | 6,552.0 | — |
| `WideToJson` | `-` | 607.6 ± 8.1 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,525.9 ± 142.6 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,832.0 ± 29.9 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 990.1 ± 8.5 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,994.1 ± 12.5 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 223.8 ± 3.7 | — | 984.0 | — |
| `decode_native` | `-` | 20.3 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 216.6 ± 0.5 | — | 984.0 | — |
| `encode_bridged` | `-` | 235.1 ± 6.8 | — | 1,277.3 | — |
| `encode_native` | `-` | 16.0 ± 0.2 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 245.2 ± 2.7 | — | 1,288.0 | — |
| `fieldGet_bridged` | `-` | 99.2 ± 1.1 | — | 432.0 | — |
| `fieldGet_native` | `-` | 98.4 ± 0.8 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 392.1 ± 7.0 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 180.5 ± 2.1 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.8 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.8 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.2 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.1 ± 0.4 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.1 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 34.3 ± 0.6 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 39.2 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.6 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.2 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 22.3 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 43.8 ± 0.8 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 22.6 ± 0.3 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.1 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.6 ± 0.3 | — | 40.0 | — |
| `reuseLens3` | `-` | 49.3 ± 1.4 | — | 72.0 | — |
| `reuseLens6` | `-` | 135.3 ± 0.5 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 63.7 ± 0.3 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 5,053.9 ± 60.7 | 4,767.8 ± 82.8 | 14,080.8 | 14,080.8 |
| `FoldMap` | `size=64` | 330.5 ± 12.6 | 318.1 ± 14.0 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.5 ± 0.0 | 22.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,159.1 ± 68.6 | 3,239.5 ± 362.6 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 378.8 ± 7.2 | 373.1 ± 0.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 48.3 ± 0.3 | 48.4 ± 0.5 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.3 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.2 ± 0.2 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.4 ± 0.2 | 8.8 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 32.3 ± 0.2 | 27.5 ± 0.9 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.8 ± 0.0 | 4.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.4 ± 0.1 | 3.3 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 437,624.1 ± 5,896.0 | — | 1,073,032.0 | — |
| `cModifyId` | `size=64` | 55,936.3 ± 266.3 | — | 136,417.0 | — |
| `cModifyId` | `size=8` | 9,614.2 ± 270.2 | — | 21,072.1 | — |
| `cReadId` | `size=512` | 225,449.6 ± 3,305.8 | — | 804,176.1 | — |
| `cReadId` | `size=64` | 28,624.9 ± 175.0 | — | 101,381.7 | — |
| `cReadId` | `size=8` | 4,600.7 ± 45.8 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 225,963.2 ± 422.4 | — | 804,352.2 | — |
| `cReadStreet` | `size=64` | 28,516.2 ± 102.3 | — | 101,550.0 | — |
| `cReadStreet` | `size=8` | 4,609.0 ± 9.6 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 436,166.4 ± 6,079.2 | — | 1,072,952.4 | — |
| `cReplaceId` | `size=64` | 56,740.4 ± 1,347.2 | — | 136,318.8 | — |
| `cReplaceId` | `size=8` | 9,532.7 ± 233.7 | — | 21,248.1 | — |
| `cSumPrices` | `size=512` | 361,357.2 ± 5,800.5 | — | 1,253,094.8 | — |
| `cSumPrices` | `size=64` | 44,912.4 ± 922.5 | — | 157,791.7 | — |
| `cSumPrices` | `size=8` | 6,643.1 ± 23.7 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 193.0 ± 0.4 | — | 0.1 | — |
| `jMiss` | `size=64` | 192.9 ± 0.4 | — | 0.0 | — |
| `jMiss` | `size=8` | 190.3 ± 4.5 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,920.8 ± 29.8 | — | 41,920.8 | — |
| `jModifyId` | `size=64` | 352.5 ± 7.2 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 106.4 ± 2.4 | — | 992.0 | — |
| `jReadId` | `size=512` | 36.8 ± 1.4 | — | 56.0 | — |
| `jReadId` | `size=64` | 37.9 ± 1.8 | — | 56.0 | — |
| `jReadId` | `size=8` | 36.8 ± 2.0 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 213.1 ± 4.2 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 215.1 ± 3.6 | — | 144.0 | — |
| `jReadStreet` | `size=8` | 210.5 ± 3.0 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 2,910.8 ± 13.3 | — | 41,888.8 | — |
| `jReplaceId` | `size=64` | 342.0 ± 10.5 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 102.7 ± 2.9 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 88,677.0 ± 1,187.9 | — | 63,663.8 | — |
| `jSumPrices` | `size=64` | 10,921.6 ± 53.8 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,481.3 ± 22.9 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.1 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 39.2 ± 0.2 | 33.0 ± 0.4 | 152.0 | 176.0 |
| `Replace` | `-` | 3.6 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 54,218.0 ± 668.4 | — | 380,269.7 | — |
| `Modify_multiFocus` | `size=256` | 13,380.3 ± 187.0 | — | 88,400.8 | — |
| `Modify_multiFocus` | `size=32` | 1,555.0 ± 20.8 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 227.2 ± 3.4 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 38,485.5 ± 626.1 | — | 119,393.7 | — |
| `Modify_powerEach` | `size=256` | 9,256.2 ± 201.8 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 888.6 ± 3.8 | — | 3,304.0 | — |
| `Modify_powerEach` | `size=4` | 219.4 ± 0.9 | — | 904.0 | — |
| `naive_listMap` | `size=1024` | 9,512.9 ± 233.1 | — | 65,578.5 | — |
| `naive_listMap` | `size=256` | 2,297.2 ± 54.3 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 265.6 ± 0.7 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 36.1 ± 1.1 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 68.7 ± 0.3 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 181.9 ± 0.4 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 16.3 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.1 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 41.9 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.1 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 15.0 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 164.3 ± 0.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 49.2 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,131.4 ± 15.6 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,060.0 ± 14.9 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.8 ± 0.1 | 23.1 ± 0.0 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 66.0 ± 3.4 | 66.4 ± 0.6 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.9 ± 1.9 | 109.9 ± 0.4 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 21.4 ± 0.4 | 20.6 ± 0.3 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.3 ± 0.1 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.9 ± 0.1 | 7.5 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 39,953.7 ± 543.7 | — | 97,420.2 | — |
| `ModifyNames` | `size=64` | 4,990.0 ± 33.9 | — | 12,621.5 | — |
| `ModifyNames` | `size=8` | 675.4 ± 8.7 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 143.5 ± 0.2 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 143.6 ± 0.4 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 143.2 ± 0.2 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 42.8 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 42.5 ± 0.1 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 42.8 ± 0.3 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,201.9 ± 244.1 | — | 382,772.0 | — |
| `monocleModifyNames` | `size=64` | 10,515.4 ± 552.4 | — | 39,848.4 | — |
| `monocleModifyNames` | `size=8` | 1,438.5 ± 16.2 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 57,592.4 ± 1,293.2 | — | 169,087.1 | — |
| `monocleModifyStreet` | `size=64` | 7,669.9 ± 151.6 | — | 20,904.3 | — |
| `monocleModifyStreet` | `size=8` | 975.5 ± 6.0 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 36,219.1 ± 466.3 | — | 69,792.5 | — |
| `monocleReadStreet` | `size=64` | 4,688.1 ± 59.7 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 549.0 ± 9.3 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 73,682.8 ± 570.1 | — | 226,301.6 | — |
| `naiveModifyNames` | `size=64` | 9,419.2 ± 16.2 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,146.1 ± 22.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 58,936.8 ± 1,601.5 | — | 169,063.8 | — |
| `naiveModifyStreet` | `size=64` | 7,256.1 ± 472.7 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 977.9 ± 17.1 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 36,345.3 ± 142.7 | — | 69,792.0 | — |
| `naiveReadStreet` | `size=64` | 4,661.6 ± 14.9 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 550.9 ± 11.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 247,200.3 ± 7,534.6 | — | 650,954.7 | — |
| `Names` | `size=64` | 30,625.4 ± 534.8 | — | 82,955.1 | — |
| `Names` | `size=8` | 4,116.7 ± 55.9 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 272,384.6 ± 5,660.3 | — | 724,591.7 | — |
| `NamesIor` | `size=64` | 33,876.4 ± 482.6 | — | 91,172.4 | — |
| `NamesIor` | `size=8` | 4,247.5 ± 32.5 | — | 12,000.1 | — |
| `Street` | `size=512` | 1,045.8 ± 6.5 | — | 2,968.8 | — |
| `Street` | `size=64` | 1,043.0 ± 12.5 | — | 2,968.1 | — |
| `Street` | `size=8` | 1,079.8 ± 29.4 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,055.1 ± 8.2 | — | 2,984.8 | — |
| `StreetIor` | `size=64` | 1,070.9 ± 28.6 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,047.6 ± 5.8 | — | 2,984.0 | — |
| `directNames` | `size=512` | 243,820.4 ± 1,814.8 | — | 609,904.0 | — |
| `directNames` | `size=64` | 29,838.7 ± 378.1 | — | 77,719.8 | — |
| `directNames` | `size=8` | 3,985.0 ± 22.6 | — | 10,693.4 | — |
| `directStreet` | `size=512` | 1,020.5 ± 15.0 | — | 2,728.8 | — |
| `directStreet` | `size=64` | 1,010.8 ± 13.7 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,008.8 ± 14.7 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 247,912.7 ± 4,460.1 | — | 613,995.2 | — |
| `hcursorNames` | `size=64` | 30,067.7 ± 506.0 | — | 77,787.2 | — |
| `hcursorNames` | `size=8` | 4,119.9 ± 74.3 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,067.4 ± 10.7 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,081.1 ± 17.0 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,071.8 ± 29.0 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 238,619.7 ± 4,117.1 | — | 1,121,768.3 | — |
| `monocleNames` | `size=64` | 25,616.5 ± 252.1 | — | 132,769.6 | — |
| `monocleNames` | `size=8` | 3,890.0 ± 92.2 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 191,359.0 ± 5,607.7 | — | 908,032.6 | — |
| `monocleStreet` | `size=64` | 22,409.8 ± 349.8 | — | 113,794.0 | — |
| `monocleStreet` | `size=8` | 3,260.0 ± 15.9 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 200,721.8 ± 3,031.5 | — | 965,270.9 | — |
| `naiveNames` | `size=64` | 24,185.6 ± 59.5 | — | 120,842.2 | — |
| `naiveNames` | `size=8` | 3,481.0 ± 59.2 | — | 17,834.7 | — |
| `naiveStreet` | `size=512` | 184,566.0 ± 2,574.1 | — | 908,028.0 | — |
| `naiveStreet` | `size=64` | 22,325.3 ± 153.9 | — | 113,791.3 | — |
| `naiveStreet` | `size=8` | 3,273.5 ± 8.4 | — | 17,056.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,218.4 ± 93.5 | — | 42,026.9 | — |
| `ModifyStreet` | `size=64` | 542.4 ± 5.3 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 304.1 ± 3.5 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 212.2 ± 1.2 | — | 114.9 | — |
| `ReadStreet` | `size=64` | 212.1 ± 1.8 | — | 114.7 | — |
| `ReadStreet` | `size=8` | 211.2 ± 2.7 | — | 128.0 | — |
| `SumPrices` | `size=512` | 88,588.3 ± 1,460.6 | — | 63,720.3 | — |
| `SumPrices` | `size=64` | 10,942.7 ± 50.8 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,460.1 ± 10.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 172,345.2 ± 413.9 | — | 333,617.4 | — |
| `monocleModifyStreet` | `size=64` | 21,631.8 ± 248.7 | — | 30,114.2 | — |
| `monocleModifyStreet` | `size=8` | 3,504.8 ± 35.4 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 98,573.0 ± 317.1 | — | 193,268.4 | — |
| `monocleReadStreet` | `size=64` | 12,466.5 ± 128.9 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,925.1 ± 52.3 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 475,261.4 ± 8,658.2 | — | 1,190,825.4 | — |
| `monocleSumPrices` | `size=64` | 17,011.5 ± 171.0 | — | 47,425.7 | — |
| `monocleSumPrices` | `size=8` | 2,624.7 ± 78.9 | — | 6,680.1 | — |
| `naiveModifyStreet` | `size=512` | 173,197.0 ± 1,782.1 | — | 333,608.7 | — |
| `naiveModifyStreet` | `size=64` | 21,636.1 ± 170.0 | — | 30,090.2 | — |
| `naiveModifyStreet` | `size=8` | 3,502.8 ± 18.4 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 98,781.8 ± 1,127.2 | — | 193,268.6 | — |
| `naiveReadStreet` | `size=64` | 12,459.0 ± 118.1 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,885.9 ± 4.8 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 103,695.5 ± 927.2 | — | 230,160.8 | — |
| `naiveSumPrices` | `size=64` | 13,059.9 ± 48.4 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 1,974.7 ± 15.5 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 42,670.6 ± 1,179.8 | — | 459.7 | — |
| `nativeReadStreet` | `size=64` | 5,316.8 ± 110.1 | — | 424.6 | — |
| `nativeReadStreet` | `size=8` | 856.8 ± 10.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 68,959.9 ± 1,153.0 | — | 86,273.9 | — |
| `nativeSumPrices` | `size=64` | 8,533.0 ± 100.2 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,279.9 ± 58.1 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 133,576.6 ± 1,254.1 | — | 624,393.2 | — |
| `TransformDeep` | `n=512` | 13,622.5 ± 159.0 | — | 57,361.4 | — |
| `TransformDeep` | `n=64` | 1,567.6 ± 4.5 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 153,927.4 ± 1,291.5 | 184,234.5 ± 465.0 | 655,376.0 | 753,750.1 |
| `TransformExpr` | `n=512` | 18,762.3 ± 90.1 | 16,632.8 ± 40.7 | 81,825.9 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,311.6 ± 31.5 | 2,855.6 ± 38.2 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 111,388.6 ± 2,308.7 | — | 786,585.1 | — |
| `UniverseDeep` | `n=512` | 16,288.5 ± 507.3 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,947.6 ± 25.5 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 102,457.5 ± 495.1 | 1,827,256.2 ± 36,763.1 | 786,386.6 | 4,752,544.8 |
| `UniverseExpr` | `n=512` | 15,567.7 ± 241.9 | 184,916.7 ± 5,947.2 | 98,185.6 | 483,202.9 |
| `UniverseExpr` | `n=64` | 1,892.5 ± 32.9 | 16,825.5 ± 558.6 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 191,605.1 ± 1,320.8 | 1,969,164.6 ± 36,638.5 | 786,451.4 | 6,488,975.9 |
| `UniverseJson` | `n=512` | 21,599.1 ± 467.5 | 207,554.4 ± 3,046.7 | 98,186.2 | 699,917.2 |
| `UniverseJson` | `n=64` | 2,512.8 ± 9.2 | 20,710.5 ± 126.8 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 44,648.7 ± 154.2 | — | 163,888.5 | — |
| `visitorTransformDeep` | `n=512` | 4,060.0 ± 32.2 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 451.9 ± 5.4 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 72,875.9 ± 221.7 | — | 360,477.0 | — |
| `visitorTransformExpr` | `n=512` | 9,047.2 ± 99.8 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,124.4 ± 3.1 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 59,761.5 ± 884.9 | — | 196,707.5 | — |
| `visitorUniverseDeep` | `n=512` | 7,499.8 ± 335.7 | — | 24,632.8 | — |
| `visitorUniverseDeep` | `n=64` | 830.8 ± 2.0 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,863.1 ± 1,526.7 | — | 196,656.7 | — |
| `visitorUniverseExpr` | `n=512` | 6,926.4 ± 18.6 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 828.3 ± 18.3 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 132,336.5 ± 3,867.3 | — | 294,992.3 | — |
| `visitorUniverseJson` | `n=512` | 14,256.5 ± 44.7 | — | 36,849.5 | — |
| `visitorUniverseJson` | `n=64` | 2,169.7 ± 291.0 | — | 4,928.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 14,717.1 ± 264.3 | — | 41,447.7 | — |
| `Modify_powerEach` | `size=16` | 295.9 ± 6.2 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,537.9 ± 51.2 | — | 10,712.6 | — |
| `Modify_powerEach` | `size=4` | 129.2 ± 0.7 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 57,535.4 ± 279.0 | — | 164,431.2 | — |
| `Modify_powerEach` | `size=64` | 938.8 ± 12.5 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 58,271.5 ± 1,279.4 | — | 279,433.1 | — |
| `monocle_powerEach` | `size=16` | 594.4 ± 30.0 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,567.2 ± 199.2 | — | 107,339.5 | — |
| `monocle_powerEach` | `size=4` | 232.6 ± 2.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 190,430.7 ± 3,433.8 | — | 967,880.8 | — |
| `monocle_powerEach` | `size=64` | 2,130.8 ± 23.5 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,834.8 ± 45.1 | — | 28,730.9 | — |
| `naive_powerEach` | `size=16` | 110.7 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,691.2 ± 13.1 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 28.6 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,163.5 ± 76.8 | — | 114,784.0 | — |
| `naive_powerEach` | `size=64` | 426.0 ± 3.6 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 67,057.4 ± 2,992.7 | — | 210,980.0 | — |
| `Modify_nested` | `size=16` | 1,618.6 ± 27.8 | — | 5,269.4 | — |
| `Modify_nested` | `size=256` | 18,081.1 ± 691.9 | — | 54,219.4 | — |
| `Modify_nested` | `size=4` | 772.3 ± 15.1 | — | 2,680.0 | — |
| `Modify_nested` | `size=64` | 4,299.9 ± 96.4 | — | 15,032.5 | — |
| `monocle_nested` | `size=1024` | 246,250.7 ± 4,646.0 | — | 1,118,872.6 | — |
| `monocle_nested` | `size=16` | 2,770.7 ± 156.2 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 93,266.9 ± 1,527.9 | — | 430,212.7 | — |
| `monocle_nested` | `size=4` | 1,198.6 ± 141.9 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,743.9 ± 210.6 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 22,408.0 ± 658.8 | — | 115,075.0 | — |
| `naive_nested` | `size=16` | 411.3 ± 7.1 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,262.3 ± 82.6 | — | 29,019.4 | — |
| `naive_nested` | `size=4` | 145.2 ± 4.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,473.2 ± 21.7 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,821.1 ± 156.9 | — | 4,933.4 | — |
| `Modify_sparse` | `size=2048` | 29,718.4 ± 506.5 | — | 104,802.9 | — |
| `Modify_sparse` | `size=32` | 546.0 ± 12.7 | — | 1,477.3 | — |
| `Modify_sparse` | `size=512` | 7,515.4 ± 65.2 | — | 24,906.1 | — |
| `Modify_sparse` | `size=8` | 173.3 ± 2.2 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,869.9 ± 56.1 | — | 24,712.2 | — |
| `monocle_sparse` | `size=2048` | 96,107.2 ± 769.2 | — | 475,348.6 | — |
| `monocle_sparse` | `size=32` | 968.1 ± 15.8 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,316.7 ± 445.6 | — | 156,442.6 | — |
| `monocle_sparse` | `size=8` | 274.2 ± 2.1 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 325.1 ± 11.4 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,595.2 ± 99.6 | — | 24,612.6 | — |
| `naive_sparse` | `size=32` | 84.1 ± 1.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,296.7 ± 27.1 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 26.0 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.1 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.8 ± 0.6 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.3 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.7 ± 0.3 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.9 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.5 ± 0.2 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 121,815.8 ± 2,015.3 | — | 786,296.9 | — |
| `Cata` | `-` | 87,029.0 ± 211.9 | — | 197,568.6 | — |
| `Hylo` | `-` | 87,552.5 ± 1,346.7 | — | 295,848.6 | — |
| `drosteAna` | `-` | 54,327.5 ± 511.1 | — | 327,632.4 | — |
| `drosteCata` | `-` | 46,989.6 ± 451.9 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,269.7 ± 1,014.7 | — | 328,640.4 | — |
| `handAna` | `-` | 21,536.1 ± 115.7 | — | 163,816.1 | — |
| `handCata` | `-` | 13,190.8 ± 303.6 | — | 0.1 | — |
| `handHylo` | `-` | 12,128.9 ± 51.5 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.2 ± 0.1 | 26.4 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.3 ± 0.3 | 52.6 ± 0.4 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.4 ± 0.1 | 3.4 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,366.6 ± 164.2 | 35,045.8 ± 2,004.6 | 38,961.1 | 176,912.5 |
| `Modify` | `size=64` | 935.1 ± 7.8 | 1,804.4 ± 3.9 | 4,864.0 | 14,448.0 |
| `Modify` | `size=8` | 127.4 ± 1.8 | 242.0 ± 0.7 | 688.0 | 1,936.0 |


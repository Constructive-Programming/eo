# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `a76044879015e5f2d4ba4cc4116e42ec2ffb7934` · date: `2026-07-16` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 2.4 ± 0.0 | 0.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.2 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.2 ± 0.1 | 10.2 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 30.4 ± 0.7 | 24.1 ± 0.4 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 2.3 ± 0.0 | 1.0 ± 0.0 | 16.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 163.1 ± 3.1 | — | 704.0 | — |
| `ModifyCountry` | `-` | 328.0 ± 6.8 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 385.4 ± 4.1 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 179.1 ± 2.4 | — | 520.0 | — |
| `ReadPartner` | `-` | 212.6 ± 3.0 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 328.8 ± 2.0 | — | 1,176.0 | — |
| `naiveModifyCountry` | `-` | 2,753.5 ± 35.5 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,701.3 ± 10.1 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,183.4 ± 23.6 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,696.7 ± 4.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,721.2 ± 22.7 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 753.1 ± 7.3 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 546.9 ± 6.7 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 421.7 ± 9.9 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 430.1 ± 4.6 | — | 1,504.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,301.5 ± 8.1 | — | 3,640.0 | — |
| `freshDecodeRecord` | `-` | 1,291.7 ± 18.0 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,566.9 ± 34.4 | — | 9,373.4 | — |
| `ClickToJson` | `-` | 2,975.5 ± 4.9 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 829.9 ± 26.2 | — | 6,552.0 | — |
| `WideToJson` | `-` | 648.4 ± 10.1 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,528.5 ± 136.8 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,847.3 ± 53.3 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 983.8 ± 11.0 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 2,013.7 ± 28.5 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 222.6 ± 2.8 | — | 984.0 | — |
| `decode_native` | `-` | 20.2 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 217.9 ± 7.1 | — | 984.0 | — |
| `encode_bridged` | `-` | 241.5 ± 2.5 | — | 1,309.3 | — |
| `encode_native` | `-` | 16.0 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 241.7 ± 7.9 | — | 1,282.7 | — |
| `fieldGet_bridged` | `-` | 97.8 ± 1.0 | — | 432.0 | — |
| `fieldGet_native` | `-` | 98.3 ± 0.4 | — | 437.3 | — |
| `rootGet_bridged` | `-` | 389.2 ± 4.0 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 182.8 ± 2.5 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.9 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.8 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 33.8 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 39.2 ± 0.4 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.3 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.5 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.1 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.3 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 22.2 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 43.0 ± 0.7 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 22.5 ± 0.1 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.6 ± 0.4 | — | 40.0 | — |
| `reuseLens3` | `-` | 48.7 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 135.1 ± 0.2 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 63.5 ± 0.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 5,027.5 ± 8.5 | 4,723.1 ± 13.0 | 14,080.8 | 14,080.8 |
| `FoldMap` | `size=64` | 324.8 ± 0.8 | 312.3 ± 0.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.5 ± 0.0 | 21.9 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,132.3 ± 34.0 | 3,164.3 ± 114.1 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 371.6 ± 0.5 | 374.4 ± 7.6 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 48.2 ± 0.4 | 48.1 ± 0.3 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.4 ± 0.1 | 8.8 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 32.3 ± 0.2 | 27.6 ± 0.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.8 ± 0.0 | 4.0 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.3 ± 0.0 | 3.3 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 434,723.2 ± 5,477.4 | — | 1,073,038.8 | — |
| `cModifyId` | `size=64` | 56,012.5 ± 613.6 | — | 136,406.6 | — |
| `cModifyId` | `size=8` | 9,690.0 ± 244.7 | — | 21,312.1 | — |
| `cReadId` | `size=512` | 227,783.6 ± 4,676.9 | — | 804,176.8 | — |
| `cReadId` | `size=64` | 28,526.1 ± 149.0 | — | 101,373.8 | — |
| `cReadId` | `size=8` | 4,576.0 ± 46.5 | — | 15,672.0 | — |
| `cReadStreet` | `size=512` | 226,266.8 ± 1,741.8 | — | 804,352.3 | — |
| `cReadStreet` | `size=64` | 28,608.9 ± 428.4 | — | 101,549.8 | — |
| `cReadStreet` | `size=8` | 4,592.8 ± 11.8 | — | 15,840.0 | — |
| `cReplaceId` | `size=512` | 434,992.4 ± 3,232.5 | — | 1,072,933.5 | — |
| `cReplaceId` | `size=64` | 56,164.3 ± 909.9 | — | 136,306.2 | — |
| `cReplaceId` | `size=8` | 9,402.2 ± 167.7 | — | 21,000.1 | — |
| `cSumPrices` | `size=512` | 358,134.1 ± 3,292.7 | — | 1,253,085.8 | — |
| `cSumPrices` | `size=64` | 44,838.1 ± 765.2 | — | 157,797.4 | — |
| `cSumPrices` | `size=8` | 6,780.2 ± 202.6 | — | 22,928.1 | — |
| `jMiss` | `size=512` | 178.9 ± 0.5 | — | 0.1 | — |
| `jMiss` | `size=64` | 180.4 ± 2.9 | — | 0.0 | — |
| `jMiss` | `size=8` | 179.6 ± 3.1 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,937.0 ± 12.3 | — | 41,944.8 | — |
| `jModifyId` | `size=64` | 325.5 ± 5.0 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 103.0 ± 2.7 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.4 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=64` | 37.2 ± 1.6 | — | 56.0 | — |
| `jReadId` | `size=8` | 37.4 ± 1.8 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 191.8 ± 1.9 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 192.2 ± 3.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 190.2 ± 0.4 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 2,781.4 ± 44.1 | — | 41,896.8 | — |
| `jReplaceId` | `size=64` | 319.5 ± 2.7 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 108.1 ± 15.8 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 87,354.9 ± 276.8 | — | 63,666.2 | — |
| `jSumPrices` | `size=64` | 10,717.3 ± 101.3 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,443.2 ± 26.5 | — | 1,176.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.1 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 39.0 ± 0.5 | 32.9 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_multiFocus` | `size=1024` | 52,806.7 ± 362.9 | — | 380,251.6 | — |
| `Modify_multiFocus` | `size=256` | 13,211.4 ± 19.0 | — | 88,400.9 | — |
| `Modify_multiFocus` | `size=32` | 1,546.8 ± 23.1 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 226.4 ± 3.5 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 38,513.5 ± 741.7 | — | 119,393.7 | — |
| `Modify_powerEach` | `size=256` | 9,344.5 ± 150.6 | — | 27,168.6 | — |
| `Modify_powerEach` | `size=32` | 1,130.7 ± 1.8 | — | 3,256.0 | — |
| `Modify_powerEach` | `size=4` | 206.4 ± 18.1 | — | 920.0 | — |
| `naive_listMap` | `size=1024` | 9,625.6 ± 668.0 | — | 65,578.5 | — |
| `naive_listMap` | `size=256` | 2,268.9 ± 8.2 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 264.9 ± 3.0 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 35.2 ± 0.1 | — | 296.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 68.5 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 183.0 ± 1.6 | — | 872.0 | — |
| `Modify_multiFocusTuple3` | `-` | 16.4 ± 0.2 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.0 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 42.1 ± 0.4 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.0 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.7 ± 0.3 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 166.4 ± 5.0 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 49.2 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,137.4 ± 25.8 | — | 3,152.0 | — |
| `reuseUse` | `-` | 1,049.1 ± 8.9 | — | 2,968.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.8 ± 0.2 | 23.1 ± 0.0 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 63.8 ± 0.3 | 66.3 ± 0.2 | 160.0 | 304.0 |
| `Modify_6` | `-` | 151.3 ± 0.3 | 109.0 ± 0.4 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 21.3 ± 0.1 | 20.5 ± 0.6 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.2 ± 0.0 | 3.7 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.7 ± 0.0 | 7.4 ± 0.2 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 39,942.4 ± 859.6 | — | 97,657.4 | — |
| `ModifyNames` | `size=64` | 4,998.6 ± 59.1 | — | 12,800.2 | — |
| `ModifyNames` | `size=8` | 685.9 ± 12.5 | — | 2,208.0 | — |
| `ModifyStreet` | `size=512` | 145.0 ± 1.2 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 144.5 ± 0.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 145.0 ± 0.3 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 45.0 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 44.8 ± 0.1 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 45.6 ± 1.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 104,269.3 ± 318.3 | — | 382,772.0 | — |
| `monocleModifyNames` | `size=64` | 10,512.5 ± 497.6 | — | 39,848.4 | — |
| `monocleModifyNames` | `size=8` | 1,504.4 ± 60.6 | — | 5,432.0 | — |
| `monocleModifyStreet` | `size=512` | 58,430.1 ± 261.7 | — | 169,084.5 | — |
| `monocleModifyStreet` | `size=64` | 7,557.7 ± 23.9 | — | 20,904.3 | — |
| `monocleModifyStreet` | `size=8` | 982.5 ± 5.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 36,314.5 ± 188.9 | — | 69,792.5 | — |
| `monocleReadStreet` | `size=64` | 4,659.7 ± 17.7 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 547.7 ± 4.0 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 73,342.8 ± 266.3 | — | 226,301.6 | — |
| `naiveModifyNames` | `size=64` | 9,392.0 ± 144.1 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,121.4 ± 11.5 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 58,212.6 ± 408.9 | — | 169,063.4 | — |
| `naiveModifyStreet` | `size=64` | 7,562.7 ± 84.3 | — | 20,880.3 | — |
| `naiveModifyStreet` | `size=8` | 985.8 ± 7.6 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 36,277.0 ± 119.3 | — | 69,792.2 | — |
| `naiveReadStreet` | `size=64` | 4,687.4 ± 56.8 | — | 8,848.2 | — |
| `naiveReadStreet` | `size=8` | 547.5 ± 5.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 244,286.6 ± 4,257.9 | — | 650,952.4 | — |
| `Names` | `size=64` | 30,389.4 ± 84.8 | — | 82,947.1 | — |
| `Names` | `size=8` | 4,145.4 ± 39.2 | — | 11,432.1 | — |
| `NamesIor` | `size=512` | 270,667.9 ± 4,947.6 | — | 724,590.5 | — |
| `NamesIor` | `size=64` | 32,973.3 ± 693.9 | — | 91,173.5 | — |
| `NamesIor` | `size=8` | 4,284.4 ± 89.7 | — | 12,104.1 | — |
| `Street` | `size=512` | 1,054.1 ± 11.5 | — | 2,968.8 | — |
| `Street` | `size=64` | 1,085.2 ± 54.7 | — | 2,960.1 | — |
| `Street` | `size=8` | 1,048.8 ± 10.7 | — | 2,968.0 | — |
| `StreetIor` | `size=512` | 1,064.7 ± 8.4 | — | 2,990.2 | — |
| `StreetIor` | `size=64` | 1,076.2 ± 32.3 | — | 2,984.1 | — |
| `StreetIor` | `size=8` | 1,068.0 ± 26.3 | — | 2,984.0 | — |
| `directNames` | `size=512` | 238,423.3 ± 5,964.2 | — | 609,884.9 | — |
| `directNames` | `size=64` | 30,624.5 ± 1,533.2 | — | 77,709.3 | — |
| `directNames` | `size=8` | 4,066.2 ± 43.6 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,008.7 ± 8.8 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,008.1 ± 8.9 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,010.1 ± 11.6 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 244,566.1 ± 2,649.3 | — | 613,992.6 | — |
| `hcursorNames` | `size=64` | 29,898.0 ± 118.1 | — | 77,786.6 | — |
| `hcursorNames` | `size=8` | 4,011.9 ± 19.9 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,069.4 ± 10.8 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,060.1 ± 10.3 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,062.5 ± 5.1 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 239,710.5 ± 4,814.9 | — | 1,121,769.1 | — |
| `monocleNames` | `size=64` | 25,613.7 ± 214.6 | — | 132,758.9 | — |
| `monocleNames` | `size=8` | 3,814.6 ± 73.0 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 188,449.4 ± 5,246.7 | — | 908,030.6 | — |
| `monocleStreet` | `size=64` | 22,307.7 ± 181.2 | — | 113,799.3 | — |
| `monocleStreet` | `size=8` | 3,252.2 ± 30.0 | — | 17,069.4 | — |
| `naiveNames` | `size=512` | 201,220.7 ± 2,751.2 | — | 965,281.9 | — |
| `naiveNames` | `size=64` | 24,172.1 ± 333.7 | — | 120,842.2 | — |
| `naiveNames` | `size=8` | 3,446.3 ± 27.7 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 182,573.3 ± 1,507.0 | — | 908,032.0 | — |
| `naiveStreet` | `size=64` | 22,280.0 ± 106.5 | — | 113,786.0 | — |
| `naiveStreet` | `size=8` | 3,240.2 ± 15.3 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,098.0 ± 34.3 | — | 42,026.8 | — |
| `ModifyStreet` | `size=64` | 506.8 ± 1.9 | — | 5,448.1 | — |
| `ModifyStreet` | `size=8` | 283.3 ± 1.7 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 191.6 ± 1.2 | — | 114.8 | — |
| `ReadStreet` | `size=64` | 192.2 ± 1.9 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 190.9 ± 0.5 | — | 128.0 | — |
| `SumPrices` | `size=512` | 87,308.2 ± 1,064.4 | — | 63,720.3 | — |
| `SumPrices` | `size=64` | 11,049.0 ± 227.6 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,495.0 ± 57.1 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 171,433.1 ± 2,216.5 | — | 333,581.3 | — |
| `monocleModifyStreet` | `size=64` | 21,613.3 ± 68.9 | — | 30,114.2 | — |
| `monocleModifyStreet` | `size=8` | 3,500.5 ± 51.8 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 98,357.8 ± 484.2 | — | 193,268.2 | — |
| `monocleReadStreet` | `size=64` | 12,389.1 ± 32.5 | — | 24,737.3 | — |
| `monocleReadStreet` | `size=8` | 1,885.8 ± 3.1 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 471,163.2 ± 5,200.1 | — | 1,190,819.4 | — |
| `monocleSumPrices` | `size=64` | 17,031.8 ± 152.5 | — | 47,420.4 | — |
| `monocleSumPrices` | `size=8` | 2,552.6 ± 7.2 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 171,862.0 ± 767.1 | — | 333,607.6 | — |
| `naiveModifyStreet` | `size=64` | 21,558.8 ± 63.9 | — | 30,090.2 | — |
| `naiveModifyStreet` | `size=8` | 3,481.2 ± 13.2 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 98,252.6 ± 289.5 | — | 193,268.1 | — |
| `naiveReadStreet` | `size=64` | 12,435.5 ± 55.6 | — | 24,737.3 | — |
| `naiveReadStreet` | `size=8` | 1,890.8 ± 6.0 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 103,334.5 ± 1,204.5 | — | 230,160.5 | — |
| `naiveSumPrices` | `size=64` | 13,003.5 ± 18.1 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 1,961.1 ± 5.5 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 40,897.5 ± 2,318.1 | — | 457.6 | — |
| `nativeReadStreet` | `size=64` | 5,225.6 ± 191.7 | — | 424.6 | — |
| `nativeReadStreet` | `size=8` | 864.0 ± 3.3 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 68,780.5 ± 825.8 | — | 86,273.7 | — |
| `nativeSumPrices` | `size=64` | 8,480.6 ± 34.0 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,239.9 ± 4.3 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 133,523.9 ± 556.0 | — | 624,393.2 | — |
| `TransformDeep` | `n=512` | 13,459.6 ± 27.5 | — | 57,361.4 | — |
| `TransformDeep` | `n=64` | 1,572.2 ± 15.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 153,035.4 ± 469.2 | 183,742.2 ± 219.1 | 655,375.3 | 753,749.7 |
| `TransformExpr` | `n=512` | 18,696.8 ± 50.4 | 16,555.0 ± 51.5 | 81,825.9 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,304.2 ± 3.5 | 2,820.7 ± 4.1 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 110,507.5 ± 1,027.3 | — | 786,584.4 | — |
| `UniverseDeep` | `n=512` | 16,128.1 ± 184.7 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,930.0 ± 5.3 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 102,225.4 ± 1,022.6 | 1,774,740.7 ± 76,271.5 | 786,386.4 | 4,752,506.7 |
| `UniverseExpr` | `n=512` | 15,480.2 ± 128.1 | 186,304.5 ± 14,136.9 | 98,185.6 | 483,203.0 |
| `UniverseExpr` | `n=64` | 1,871.9 ± 3.0 | 17,217.2 ± 595.6 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 190,736.0 ± 1,075.1 | 2,083,368.9 ± 14,854.7 | 786,450.8 | 6,489,058.5 |
| `UniverseJson` | `n=512` | 21,346.6 ± 146.9 | 211,520.0 ± 8,383.0 | 98,186.2 | 699,917.6 |
| `UniverseJson` | `n=64` | 2,503.1 ± 6.2 | 20,357.7 ± 241.3 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 44,794.5 ± 674.6 | — | 163,888.6 | — |
| `visitorTransformDeep` | `n=512` | 4,091.0 ± 49.4 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 451.8 ± 3.6 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 72,923.8 ± 311.2 | — | 360,477.1 | — |
| `visitorTransformExpr` | `n=512` | 9,151.0 ± 481.1 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,117.1 ± 2.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 58,860.5 ± 541.6 | — | 196,706.8 | — |
| `visitorUniverseDeep` | `n=512` | 7,329.8 ± 79.9 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 838.7 ± 32.1 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,175.3 ± 156.7 | — | 196,656.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,916.4 ± 72.6 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 824.9 ± 11.0 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 131,495.6 ± 3,397.6 | — | 294,991.7 | — |
| `visitorUniverseJson` | `n=512` | 14,382.9 ± 261.9 | — | 36,849.5 | — |
| `visitorUniverseJson` | `n=64` | 2,367.5 ± 272.1 | — | 5,216.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 14,537.8 ± 151.7 | — | 41,447.6 | — |
| `Modify_powerEach` | `size=16` | 295.8 ± 6.8 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,518.0 ± 45.8 | — | 10,712.6 | — |
| `Modify_powerEach` | `size=4` | 129.9 ± 2.2 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 57,304.3 ± 267.2 | — | 164,430.8 | — |
| `Modify_powerEach` | `size=64` | 933.7 ± 2.5 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 57,430.3 ± 781.3 | — | 279,432.5 | — |
| `monocle_powerEach` | `size=16` | 605.8 ± 28.1 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,387.1 ± 53.6 | — | 107,339.4 | — |
| `monocle_powerEach` | `size=4` | 235.0 ± 9.1 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 181,511.5 ± 4,197.0 | — | 967,865.4 | — |
| `monocle_powerEach` | `size=64` | 2,118.8 ± 40.1 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,861.6 ± 125.4 | — | 28,730.9 | — |
| `naive_powerEach` | `size=16` | 113.9 ± 5.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,690.3 ± 11.0 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 28.7 ± 0.8 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,103.9 ± 84.8 | — | 114,783.9 | — |
| `naive_powerEach` | `size=64` | 425.0 ± 0.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 65,295.0 ± 1,486.6 | — | 210,928.8 | — |
| `Modify_nested` | `size=16` | 1,635.2 ± 18.6 | — | 5,282.7 | — |
| `Modify_nested` | `size=256` | 17,288.8 ± 893.0 | — | 54,170.8 | — |
| `Modify_nested` | `size=4` | 769.6 ± 13.9 | — | 2,728.0 | — |
| `Modify_nested` | `size=64` | 4,301.4 ± 244.1 | — | 15,128.5 | — |
| `monocle_nested` | `size=1024` | 248,710.5 ± 6,832.0 | — | 1,118,877.2 | — |
| `monocle_nested` | `size=16` | 2,799.2 ± 343.7 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 92,650.7 ± 1,282.4 | — | 430,212.6 | — |
| `monocle_nested` | `size=4` | 1,206.0 ± 149.7 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,738.0 ± 37.5 | — | 58,913.0 | — |
| `naive_nested` | `size=1024` | 22,421.7 ± 539.4 | — | 115,075.0 | — |
| `naive_nested` | `size=16` | 411.8 ± 9.3 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,207.6 ± 46.3 | — | 29,019.4 | — |
| `naive_nested` | `size=4` | 145.0 ± 4.5 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,469.9 ± 19.9 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,619.2 ± 13.5 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 29,809.5 ± 1,807.5 | — | 104,803.5 | — |
| `Modify_sparse` | `size=32` | 538.5 ± 7.1 | — | 1,480.0 | — |
| `Modify_sparse` | `size=512` | 7,461.9 ± 70.3 | — | 24,906.0 | — |
| `Modify_sparse` | `size=8` | 172.5 ± 1.4 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,823.5 ± 70.3 | — | 25,058.8 | — |
| `monocle_sparse` | `size=2048` | 97,982.9 ± 3,721.4 | — | 476,032.4 | — |
| `monocle_sparse` | `size=32` | 959.1 ± 15.2 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 31,556.5 ± 351.9 | — | 156,442.6 | — |
| `monocle_sparse` | `size=8` | 276.0 ± 2.8 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 322.3 ± 0.6 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,523.7 ± 63.3 | — | 24,612.5 | — |
| `naive_sparse` | `size=32` | 83.4 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,281.6 ± 3.3 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.9 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.4 ± 0.5 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.7 ± 0.5 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.9 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.3 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 120,805.1 ± 675.2 | — | 786,296.8 | — |
| `Cata` | `-` | 87,742.5 ± 937.1 | — | 197,568.6 | — |
| `Hylo` | `-` | 83,966.5 ± 826.2 | — | 295,848.6 | — |
| `drosteAna` | `-` | 51,982.2 ± 3,238.8 | — | 327,632.4 | — |
| `drosteCata` | `-` | 46,931.5 ± 387.0 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,197.3 ± 1,070.4 | — | 328,640.4 | — |
| `handAna` | `-` | 23,554.5 ± 1,691.3 | — | 163,816.2 | — |
| `handCata` | `-` | 12,953.0 ± 20.4 | — | 0.1 | — |
| `handHylo` | `-` | 12,128.7 ± 50.8 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.3 ± 0.0 | 2.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.2 ± 0.2 | 26.4 ± 0.0 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.1 ± 0.4 | 52.6 ± 0.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify` | `size=512` | 8,764.5 ± 193.8 | 34,769.9 ± 1,666.6 | 39,001.2 | 176,913.2 |
| `Modify` | `size=64` | 982.3 ± 6.9 | 1,778.0 ± 4.6 | 4,904.0 | 14,448.0 |
| `Modify` | `size=8` | 114.7 ± 0.1 | 242.5 ± 1.7 | 728.0 | 1,936.0 |


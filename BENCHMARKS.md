# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `0be6f286b09914122c52c12626638df897d7d359` · date: `2026-07-29` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.0 ± 0.3 | 10.2 ± 0.2 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 31.6 ± 0.3 | 23.4 ± 0.2 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 168.7 ± 4.4 | — | 720.0 | — |
| `ModifyCountry` | `-` | 365.9 ± 5.7 | — | 3,184.0 | — |
| `ModifyPartner` | `-` | 454.8 ± 8.6 | — | 3,240.0 | — |
| `ReadCountry` | `-` | 170.6 ± 3.1 | — | 520.0 | — |
| `ReadPartner` | `-` | 208.5 ± 3.7 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 318.9 ± 3.4 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,691.7 ± 15.8 | — | 7,600.0 | — |
| `naiveModifyPartner` | `-` | 2,657.8 ± 30.7 | — | 7,520.0 | — |
| `naivePassthroughPayload` | `-` | 4,142.5 ± 139.1 | — | 10,584.1 | — |
| `naiveReadCountry` | `-` | 1,722.8 ± 11.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,719.6 ± 13.6 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 767.5 ± 7.0 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 590.0 ± 11.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 407.3 ± 3.7 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 426.3 ± 3.1 | — | 1,522.7 | — |
| `confluentRecordReaderFresh` | `-` | 1,322.6 ± 15.6 | — | 3,677.3 | — |
| `freshDecodeRecord` | `-` | 1,297.6 ± 6.0 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,479.7 ± 56.1 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,939.5 ± 28.6 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 1,021.1 ± 11.5 | — | 6,552.0 | — |
| `WideToJson` | `-` | 761.3 ± 24.1 | — | 1,424.0 | — |
| `naiveClickToAvro` | `-` | 1,767.1 ± 15.9 | — | 3,912.0 | — |
| `naiveClickToJson` | `-` | 2,804.0 ± 57.4 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 1,025.6 ± 16.1 | — | 3,488.0 | — |
| `naiveWideToJson` | `-` | 1,980.6 ± 56.9 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 226.0 ± 1.3 | — | 984.0 | — |
| `decode_native` | `-` | 19.1 ± 0.8 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 220.6 ± 0.6 | — | 984.0 | — |
| `encode_bridged` | `-` | 259.4 ± 2.4 | — | 1,288.0 | — |
| `encode_native` | `-` | 14.9 ± 0.3 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 259.2 ± 5.0 | — | 1,277.3 | — |
| `fieldGet_bridged` | `-` | 95.8 ± 0.3 | — | 432.0 | — |
| `fieldGet_native` | `-` | 102.2 ± 5.4 | — | 437.3 | — |
| `rootGet_bridged` | `-` | 454.9 ± 9.0 | — | 1,576.0 | — |
| `rootGet_native` | `-` | 180.2 ± 3.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 20.3 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 22.0 ± 1.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 19.8 ± 0.2 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.7 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 32.8 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 34.4 ± 0.0 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.3 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 4.9 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 41.4 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 21.4 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.2 ± 0.9 | — | 40.0 | — |
| `reuseLens3` | `-` | 47.2 ± 0.9 | — | 72.0 | — |
| `reuseLens6` | `-` | 133.5 ± 0.7 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 61.5 ± 0.1 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 4,777.0 ± 8.2 | 4,530.6 ± 74.1 | 14,080.8 | 14,080.7 |
| `FoldMap` | `size=64` | 325.3 ± 0.4 | 309.0 ± 9.3 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.0 ± 0.0 | 20.4 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,781.5 ± 6.2 | 2,782.1 ± 26.0 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 350.8 ± 1.6 | 355.0 ± 3.5 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.8 ± 0.8 | 44.7 ± 0.4 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 17.0 ± 0.1 | 8.1 ± 0.0 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.2 ± 0.1 | 26.0 ± 1.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.0 | 3.7 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 411,456.6 ± 8,544.1 | — | 1,072,929.3 | — |
| `cModifyId` | `size=64` | 52,345.6 ± 352.5 | — | 136,324.5 | — |
| `cModifyId` | `size=8` | 8,880.2 ± 92.9 | — | 20,776.1 | — |
| `cReadId` | `size=512` | 223,091.3 ± 7,459.2 | — | 804,119.4 | — |
| `cReadId` | `size=64` | 27,657.4 ± 218.9 | — | 101,325.4 | — |
| `cReadId` | `size=8` | 4,418.7 ± 92.9 | — | 15,608.0 | — |
| `cReadStreet` | `size=512` | 221,925.1 ± 4,736.4 | — | 804,119.1 | — |
| `cReadStreet` | `size=64` | 27,758.3 ± 798.6 | — | 101,317.2 | — |
| `cReadStreet` | `size=8` | 4,543.6 ± 78.4 | — | 15,608.0 | — |
| `cReplaceId` | `size=512` | 412,809.8 ± 6,038.3 | — | 1,072,862.1 | — |
| `cReplaceId` | `size=64` | 52,670.2 ± 1,164.7 | — | 136,253.7 | — |
| `cReplaceId` | `size=8` | 8,878.2 ± 116.9 | — | 20,920.1 | — |
| `cSumPrices` | `size=512` | 354,085.7 ± 6,428.6 | — | 1,250,965.1 | — |
| `cSumPrices` | `size=64` | 43,493.4 ± 714.3 | — | 157,542.5 | — |
| `cSumPrices` | `size=8` | 6,465.5 ± 65.8 | — | 22,784.1 | — |
| `jMiss` | `size=512` | 175.4 ± 5.1 | — | 0.0 | — |
| `jMiss` | `size=64` | 178.9 ± 6.0 | — | 0.0 | — |
| `jMiss` | `size=8` | 178.3 ± 5.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,362.1 ± 71.2 | — | 41,921.0 | — |
| `jModifyId` | `size=64` | 424.7 ± 6.1 | — | 5,344.0 | — |
| `jModifyId` | `size=8` | 109.1 ± 3.8 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.1 ± 1.9 | — | 56.0 | — |
| `jReadId` | `size=64` | 34.3 ± 0.2 | — | 48.0 | — |
| `jReadId` | `size=8` | 35.7 ± 2.1 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 197.4 ± 1.1 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 198.6 ± 2.6 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 198.0 ± 2.0 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,296.2 ± 23.5 | — | 41,888.9 | — |
| `jReplaceId` | `size=64` | 414.2 ± 6.3 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 104.7 ± 4.4 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 85,756.9 ± 444.1 | — | 63,665.8 | — |
| `jSumPrices` | `size=64` | 10,540.1 ± 166.4 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,448.8 ± 18.4 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 69.6 ± 0.6 | — | 312.0 | — |
| `MapDrillModify` | `-` | 43.2 ± 2.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.6 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 55.6 ± 0.2 | — | 200.0 | — |
| `handEnvUse` | `-` | 66.3 ± 0.2 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 37.5 ± 0.4 | — | 216.0 | — |
| `handMapGet` | `-` | 2.1 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 53.4 ± 0.9 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.0 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 34.5 ± 0.2 | 31.5 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 15,649.8 ± 602.0 | — | 43,036.1 | — |
| `Fold_powerEach` | `size=256` | 3,623.2 ± 40.5 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 460.7 ± 3.8 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 85.1 ± 0.4 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 53,643.5 ± 765.5 | — | 380,249.6 | — |
| `Modify_multiFocus` | `size=256` | 13,052.1 ± 200.6 | — | 88,400.9 | — |
| `Modify_multiFocus` | `size=32` | 1,558.0 ± 22.1 | — | 10,136.0 | — |
| `Modify_multiFocus` | `size=4` | 249.7 ± 3.0 | — | 1,512.0 | — |
| `Modify_powerEach` | `size=1024` | 37,373.6 ± 4,069.1 | — | 115,201.7 | — |
| `Modify_powerEach` | `size=256` | 8,640.3 ± 92.9 | — | 26,104.6 | — |
| `Modify_powerEach` | `size=32` | 1,000.6 ± 126.9 | — | 3,192.0 | — |
| `Modify_powerEach` | `size=4` | 208.0 ± 17.4 | — | 856.0 | — |
| `naive_listMap` | `size=1024` | 8,319.3 ± 40.4 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,054.1 ± 10.4 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 249.1 ± 2.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.2 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,297.9 ± 42.0 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 776.1 ± 15.3 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 69.3 ± 0.2 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.4 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.9 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 167.4 ± 0.5 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 15.8 ± 0.4 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.8 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 37.4 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.5 ± 0.4 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.7 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 145.6 ± 1.0 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.3 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,158.0 ± 10.1 | — | 2,816.0 | — |
| `reuseUse` | `-` | 1,093.6 ± 16.1 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 23.0 ± 0.3 | 22.4 ± 0.2 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 70.0 ± 11.9 | 71.6 ± 1.3 | 160.0 | 304.0 |
| `Modify_6` | `-` | 152.5 ± 1.5 | 116.7 ± 1.8 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 20.8 ± 0.3 | 20.7 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.0 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.1 ± 0.1 | 6.9 ± 0.2 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 37,855.8 ± 745.8 | — | 101,531.1 | — |
| `ModifyNames` | `size=64` | 4,734.6 ± 87.3 | — | 13,080.2 | — |
| `ModifyNames` | `size=8` | 650.0 ± 8.5 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 135.4 ± 1.6 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 134.3 ± 0.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 134.7 ± 1.4 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 39.4 ± 0.8 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 39.9 ± 0.9 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 39.0 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 102,484.1 ± 860.4 | — | 382,772.7 | — |
| `monocleModifyNames` | `size=64` | 10,131.8 ± 316.1 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,568.2 ± 42.0 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 55,102.9 ± 353.6 | — | 169,084.4 | — |
| `monocleModifyStreet` | `size=64` | 6,942.6 ± 503.3 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 1,043.0 ± 22.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,663.7 ± 176.6 | — | 69,789.7 | — |
| `monocleReadStreet` | `size=64` | 4,201.4 ± 29.2 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 520.0 ± 7.7 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 69,218.3 ± 281.4 | — | 226,301.3 | — |
| `naiveModifyNames` | `size=64` | 9,041.9 ± 68.4 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,225.2 ± 17.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,638.3 ± 1,807.1 | — | 169,062.0 | — |
| `naiveModifyStreet` | `size=64` | 7,204.6 ± 59.1 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 1,026.9 ± 4.2 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,981.4 ± 259.5 | — | 69,789.4 | — |
| `naiveReadStreet` | `size=64` | 4,223.2 ± 49.6 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 513.0 ± 5.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 257,292.2 ± 3,740.4 | — | 614,042.6 | — |
| `Names` | `size=64` | 33,540.4 ± 1,006.6 | — | 79,299.5 | — |
| `Names` | `size=8` | 4,634.5 ± 66.4 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 281,553.7 ± 3,694.1 | — | 671,268.3 | — |
| `NamesIor` | `size=64` | 36,388.2 ± 630.9 | — | 87,533.7 | — |
| `NamesIor` | `size=8` | 4,969.3 ± 90.6 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,092.2 ± 41.1 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,088.6 ± 9.4 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,080.9 ± 15.6 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,097.8 ± 28.3 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,112.6 ± 25.5 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,118.0 ± 9.3 | — | 2,736.0 | — |
| `directNames` | `size=512` | 263,175.6 ± 4,075.9 | — | 614,023.2 | — |
| `directNames` | `size=64` | 33,434.5 ± 638.7 | — | 77,728.2 | — |
| `directNames` | `size=8` | 4,399.4 ± 48.3 | — | 10,704.1 | — |
| `directStreet` | `size=512` | 1,088.1 ± 29.2 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,086.4 ± 28.4 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,105.0 ± 16.8 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 255,680.5 ± 3,073.4 | — | 605,793.3 | — |
| `hcursorNames` | `size=64` | 32,636.4 ± 669.8 | — | 77,792.6 | — |
| `hcursorNames` | `size=8` | 4,375.4 ± 42.9 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,199.2 ± 16.4 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,182.6 ± 22.2 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,192.5 ± 33.7 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 236,872.4 ± 4,453.9 | — | 1,121,772.5 | — |
| `monocleNames` | `size=64` | 25,993.3 ± 288.6 | — | 132,760.0 | — |
| `monocleNames` | `size=8` | 4,026.9 ± 26.2 | — | 19,493.4 | — |
| `monocleStreet` | `size=512` | 191,290.9 ± 4,006.3 | — | 908,032.5 | — |
| `monocleStreet` | `size=64` | 22,810.0 ± 302.8 | — | 113,804.7 | — |
| `monocleStreet` | `size=8` | 3,461.6 ± 42.0 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 203,611.6 ± 2,311.5 | — | 965,278.1 | — |
| `naiveNames` | `size=64` | 24,382.0 ± 124.7 | — | 120,831.9 | — |
| `naiveNames` | `size=8` | 3,809.1 ± 272.2 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 192,412.4 ± 3,882.8 | — | 908,038.6 | — |
| `naiveStreet` | `size=64` | 22,683.5 ± 83.2 | — | 113,802.0 | — |
| `naiveStreet` | `size=8` | 3,435.9 ± 18.4 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,649.2 ± 43.1 | — | 42,027.3 | — |
| `ModifyStreet` | `size=64` | 605.1 ± 10.2 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 323.6 ± 10.3 | — | 1,088.0 | — |
| `ReadStreet` | `size=512` | 198.9 ± 2.8 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 200.8 ± 4.0 | — | 114.7 | — |
| `ReadStreet` | `size=8` | 202.8 ± 5.4 | — | 114.7 | — |
| `SumPrices` | `size=512` | 86,546.0 ± 1,065.1 | — | 63,719.2 | — |
| `SumPrices` | `size=64` | 10,513.5 ± 76.7 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,449.1 ± 20.3 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 165,918.7 ± 780.0 | — | 333,542.7 | — |
| `monocleModifyStreet` | `size=64` | 20,625.2 ± 127.4 | — | 30,114.1 | — |
| `monocleModifyStreet` | `size=8` | 3,417.5 ± 35.6 | — | 4,696.1 | — |
| `monocleReadStreet` | `size=512` | 95,966.8 ± 1,225.7 | — | 193,266.1 | — |
| `monocleReadStreet` | `size=64` | 12,215.4 ± 207.5 | — | 24,737.2 | — |
| `monocleReadStreet` | `size=8` | 1,960.9 ± 43.2 | — | 3,680.0 | — |
| `monocleSumPrices` | `size=512` | 448,757.1 ± 2,186.3 | — | 1,190,800.8 | — |
| `monocleSumPrices` | `size=64` | 16,840.9 ± 157.9 | — | 47,407.1 | — |
| `monocleSumPrices` | `size=8` | 2,632.8 ± 28.5 | — | 6,672.1 | — |
| `naiveModifyStreet` | `size=512` | 166,143.8 ± 1,120.7 | — | 333,569.5 | — |
| `naiveModifyStreet` | `size=64` | 20,811.5 ± 300.4 | — | 30,090.1 | — |
| `naiveModifyStreet` | `size=8` | 3,417.2 ± 28.9 | — | 4,672.1 | — |
| `naiveReadStreet` | `size=512` | 96,566.9 ± 1,291.9 | — | 193,266.7 | — |
| `naiveReadStreet` | `size=64` | 12,215.5 ± 137.9 | — | 24,737.2 | — |
| `naiveReadStreet` | `size=8` | 1,930.8 ± 13.2 | — | 3,680.0 | — |
| `naiveSumPrices` | `size=512` | 101,318.3 ± 1,125.2 | — | 230,158.7 | — |
| `naiveSumPrices` | `size=64` | 12,829.2 ± 93.0 | — | 29,369.3 | — |
| `naiveSumPrices` | `size=8` | 2,033.1 ± 19.3 | — | 4,280.0 | — |
| `nativeReadStreet` | `size=512` | 37,468.6 ± 238.1 | — | 454.8 | — |
| `nativeReadStreet` | `size=64` | 4,784.6 ± 33.4 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 803.7 ± 2.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,603.1 ± 1,116.9 | — | 86,269.5 | — |
| `nativeSumPrices` | `size=64` | 8,034.4 ± 91.9 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,192.9 ± 14.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 131,752.4 ± 1,137.1 | — | 624,391.9 | — |
| `TransformDeep` | `n=512` | 12,902.5 ± 81.3 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,584.7 ± 8.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 151,038.5 ± 4,515.5 | 177,518.7 ± 1,701.5 | 655,373.9 | 753,745.2 |
| `TransformExpr` | `n=512` | 18,238.0 ± 336.5 | 16,056.8 ± 94.7 | 81,825.9 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,243.5 ± 10.1 | 2,726.2 ± 28.7 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 102,267.3 ± 5,888.6 | — | 786,578.4 | — |
| `UniverseDeep` | `n=512` | 15,439.4 ± 79.8 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,910.3 ± 12.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 99,164.7 ± 646.2 | 1,707,195.0 ± 94,437.5 | 786,384.2 | 4,752,457.6 |
| `UniverseExpr` | `n=512` | 16,049.9 ± 1,664.7 | 175,653.8 ± 6,477.9 | 98,185.6 | 483,201.9 |
| `UniverseExpr` | `n=64` | 1,845.5 ± 11.7 | 16,845.5 ± 579.5 | 12,168.0 | 46,448.3 |
| `UniverseJson` | `n=4096` | 232,957.6 ± 1,288.3 | 2,072,227.0 ± 90,722.4 | 786,481.5 | 6,489,050.4 |
| `UniverseJson` | `n=512` | 27,229.8 ± 402.1 | 212,699.1 ± 629.4 | 98,186.8 | 699,917.7 |
| `UniverseJson` | `n=64` | 3,301.1 ± 22.4 | 21,186.3 ± 181.9 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 35,048.7 ± 265.7 | — | 163,881.5 | — |
| `visitorTransformDeep` | `n=512` | 4,123.5 ± 27.8 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 433.9 ± 1.9 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 67,362.0 ± 598.0 | — | 360,473.0 | — |
| `visitorTransformExpr` | `n=512` | 8,399.9 ± 73.4 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,042.7 ± 7.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 56,728.8 ± 564.2 | — | 196,705.3 | — |
| `visitorUniverseDeep` | `n=512` | 6,873.6 ± 58.7 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 806.4 ± 5.7 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 55,308.9 ± 364.6 | — | 196,656.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,838.1 ± 35.7 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 838.0 ± 4.4 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 165,117.7 ± 23,175.6 | — | 332,456.2 | — |
| `visitorUniverseJson` | `n=512` | 13,915.2 ± 31.1 | — | 36,849.4 | — |
| `visitorUniverseJson` | `n=64` | 2,139.3 ± 251.5 | — | 4,928.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 14,188.5 ± 116.8 | — | 41,438.7 | — |
| `Modify_powerEach` | `size=16` | 294.7 ± 2.4 | — | 1,112.0 | — |
| `Modify_powerEach` | `size=256` | 3,427.8 ± 37.8 | — | 10,712.5 | — |
| `Modify_powerEach` | `size=4` | 148.2 ± 7.2 | — | 632.0 | — |
| `Modify_powerEach` | `size=4096` | 57,627.6 ± 929.0 | — | 164,405.9 | — |
| `Modify_powerEach` | `size=64` | 907.4 ± 2.4 | — | 3,032.0 | — |
| `monocle_powerEach` | `size=1024` | 60,299.9 ± 1,600.6 | — | 279,433.8 | — |
| `monocle_powerEach` | `size=16` | 635.9 ± 11.8 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,900.5 ± 298.9 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 260.8 ± 5.1 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 188,816.7 ± 7,900.7 | — | 967,859.5 | — |
| `monocle_powerEach` | `size=64` | 2,079.5 ± 20.5 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,537.8 ± 65.1 | — | 28,730.6 | — |
| `naive_powerEach` | `size=16` | 101.7 ± 0.9 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,616.5 ± 11.6 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.1 ± 0.3 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,351.0 ± 104.7 | — | 114,780.7 | — |
| `naive_powerEach` | `size=64` | 394.6 ± 2.2 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 64,527.2 ± 829.8 | — | 210,640.2 | — |
| `Modify_nested` | `size=16` | 1,571.3 ± 12.6 | — | 4,912.1 | — |
| `Modify_nested` | `size=256` | 18,734.3 ± 260.7 | — | 53,859.7 | — |
| `Modify_nested` | `size=4` | 764.0 ± 5.1 | — | 2,408.0 | — |
| `Modify_nested` | `size=64` | 4,799.7 ± 49.4 | — | 14,757.9 | — |
| `monocle_nested` | `size=1024` | 253,177.2 ± 2,578.2 | — | 1,118,860.9 | — |
| `monocle_nested` | `size=16` | 2,917.7 ± 58.1 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 98,898.3 ± 3,265.0 | — | 430,212.3 | — |
| `monocle_nested` | `size=4` | 1,451.3 ± 127.6 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,946.2 ± 163.4 | — | 58,902.3 | — |
| `naive_nested` | `size=1024` | 21,645.9 ± 999.4 | — | 115,071.4 | — |
| `naive_nested` | `size=16` | 391.0 ± 8.6 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,830.1 ± 39.9 | — | 29,019.0 | — |
| `naive_nested` | `size=4` | 135.1 ± 3.6 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,423.4 ± 16.8 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,556.0 ± 11.3 | — | 4,936.1 | — |
| `Modify_sparse` | `size=2048` | 28,693.2 ± 309.4 | — | 104,801.3 | — |
| `Modify_sparse` | `size=32` | 526.4 ± 7.9 | — | 1,477.3 | — |
| `Modify_sparse` | `size=512` | 7,153.2 ± 57.2 | — | 24,906.0 | — |
| `Modify_sparse` | `size=8` | 174.7 ± 1.4 | — | 616.0 | — |
| `monocle_sparse` | `size=128` | 3,762.9 ± 29.3 | — | 25,064.2 | — |
| `monocle_sparse` | `size=2048` | 102,051.4 ± 1,495.0 | — | 475,350.8 | — |
| `monocle_sparse` | `size=32` | 991.7 ± 7.6 | — | 6,264.0 | — |
| `monocle_sparse` | `size=512` | 32,146.0 ± 523.6 | — | 156,443.3 | — |
| `monocle_sparse` | `size=8` | 319.9 ± 6.8 | — | 1,752.0 | — |
| `naive_sparse` | `size=128` | 327.3 ± 2.2 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,439.4 ± 62.0 | — | 24,612.4 | — |
| `naive_sparse` | `size=32` | 84.1 ± 0.3 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,292.8 ± 8.7 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.2 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.1 ± 0.0 | 2.3 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 21.3 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 37.1 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.7 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 143,897.2 ± 1,211.3 | — | 786,297.0 | — |
| `Cata` | `-` | 83,246.1 ± 1,069.4 | — | 197,568.6 | — |
| `Hylo` | `-` | 88,460.4 ± 1,577.9 | — | 295,848.6 | — |
| `drosteAna` | `-` | 57,273.9 ± 427.7 | — | 327,632.4 | — |
| `drosteCata` | `-` | 44,868.3 ± 504.5 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 76,360.0 ± 216.9 | — | 328,640.5 | — |
| `handAna` | `-` | 19,507.8 ± 277.5 | — | 163,816.1 | — |
| `handCata` | `-` | 13,127.3 ± 44.5 | — | 0.1 | — |
| `handHylo` | `-` | 11,302.9 ± 309.7 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.2 ± 0.0 | 2.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.6 ± 0.1 | 26.2 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.1 ± 0.3 | 59.9 ± 0.5 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.1 ± 0.0 | 3.0 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,547.6 ± 308.7 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 600.1 ± 6.3 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 58.0 ± 4.4 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 2,803.3 ± 26.2 | 32,269.9 ± 411.7 | 12,312.4 | 162,581.1 |
| `FoldPrices` | `size=64` | 349.9 ± 0.6 | 2,256.3 ± 9.1 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 44.7 ± 0.3 | 334.0 ± 16.8 | 216.0 | 2,010.7 |
| `Modify` | `size=512` | 7,818.2 ± 124.5 | 35,841.4 ± 331.2 | 36,897.2 | 176,925.5 |
| `Modify` | `size=64` | 855.3 ± 3.0 | 1,779.3 ± 8.2 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 102.4 ± 0.9 | 289.5 ± 3.5 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 12.0 ± 0.2 | — | 0.0 | — |
| `DrillModify` | `-` | 116.3 ± 0.6 | — | 528.0 | — |
| `ServiceGet` | `-` | 5.4 ± 0.1 | — | 0.0 | — |
| `ServiceReplace` | `-` | 83.3 ± 0.6 | — | 456.0 | — |
| `handDrillGet` | `-` | 13.1 ± 1.0 | — | 120.0 | — |
| `handDrillModify` | `-` | 109.9 ± 0.4 | — | 648.0 | — |
| `handServiceGet` | `-` | 12.5 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 96.0 ± 0.8 | — | 576.0 | — |


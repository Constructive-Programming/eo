# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `175706d88e62c7fae8ab8daec8ba09a616094467` · date: `2026-08-09` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.7 ± 0.1 | 9.7 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 33.1 ± 1.1 | 25.0 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.1 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 163.0 ± 2.2 | — | 720.0 | — |
| `ModifyCountry` | `-` | 354.3 ± 10.4 | — | 3,200.0 | — |
| `ModifyPartner` | `-` | 445.2 ± 11.9 | — | 3,266.7 | — |
| `ReadCountry` | `-` | 173.2 ± 3.5 | — | 520.0 | — |
| `ReadPartner` | `-` | 208.0 ± 8.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 338.1 ± 8.9 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,573.0 ± 4.4 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,667.7 ± 25.3 | — | 7,536.0 | — |
| `naivePassthroughPayload` | `-` | 3,963.4 ± 26.7 | — | 10,600.1 | — |
| `naiveReadCountry` | `-` | 1,621.5 ± 10.3 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,751.2 ± 22.0 | — | 4,264.0 | — |
| `prunedReadCountry` | `-` | 747.9 ± 11.9 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 561.6 ± 5.9 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 392.3 ± 0.8 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 412.7 ± 8.7 | — | 1,565.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,352.9 ± 40.5 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,343.7 ± 11.3 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,101.5 ± 28.5 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 2,881.1 ± 58.9 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 955.6 ± 9.5 | — | 6,552.0 | — |
| `WideToJson` | `-` | 676.7 ± 18.0 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,473.9 ± 10.1 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,720.3 ± 25.4 | — | 4,696.0 | — |
| `naiveWideToAvro` | `-` | 988.8 ± 40.8 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,854.9 ± 39.8 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 227.4 ± 7.2 | — | 880.0 | — |
| `decode_native` | `-` | 18.6 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 214.0 ± 1.2 | — | 880.0 | — |
| `encode_bridged` | `-` | 265.1 ± 10.2 | — | 1,256.0 | — |
| `encode_native` | `-` | 12.0 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 263.3 ± 5.2 | — | 1,261.3 | — |
| `fieldGet_bridged` | `-` | 96.2 ± 0.3 | — | 432.0 | — |
| `fieldGet_native` | `-` | 96.7 ± 0.6 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 426.1 ± 6.2 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 173.9 ± 3.2 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 22.3 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.2 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.7 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.2 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.2 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.9 ± 0.3 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.2 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.5 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.8 ± 0.0 | — | 40.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.0 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 20.2 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 35.5 ± 0.6 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 17.1 ± 0.6 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.4 ± 0.0 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.9 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 131.8 ± 1.9 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 58.2 ± 0.3 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,991.9 ± 10.3 | 3,980.9 ± 21.9 | 14,080.7 | 14,080.6 |
| `FoldMap` | `size=64` | 371.0 ± 2.2 | 341.0 ± 0.7 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.1 ± 0.0 | 22.6 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,750.7 ± 5.1 | 2,771.8 ± 5.5 | 12,312.4 | 12,312.5 |
| `FoldPrices` | `size=64` | 349.7 ± 2.3 | 350.2 ± 0.3 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 43.9 ± 0.2 | 44.3 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.4 ± 0.1 | 9.0 ± 0.7 | 0.0 | 0.0 |
| `Get_6` | `-` | 34.9 ± 0.2 | 27.5 ± 2.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.9 ± 0.0 | 3.0 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 412,548.8 ± 4,799.5 | — | 1,066,721.6 | — |
| `cModifyId` | `size=64` | 52,305.0 ± 399.3 | — | 136,269.9 | — |
| `cModifyId` | `size=8` | 8,756.4 ± 66.6 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 212,532.3 ± 7,973.7 | — | 797,932.4 | — |
| `cReadId` | `size=64` | 26,082.5 ± 260.5 | — | 101,291.7 | — |
| `cReadId` | `size=8` | 4,172.9 ± 60.4 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 211,547.1 ± 2,010.3 | — | 797,932.2 | — |
| `cReadStreet` | `size=64` | 27,230.6 ± 1,129.1 | — | 101,292.7 | — |
| `cReadStreet` | `size=8` | 4,117.4 ± 53.2 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 408,118.5 ± 4,861.6 | — | 1,066,673.1 | — |
| `cReplaceId` | `size=64` | 52,572.0 ± 271.1 | — | 136,213.7 | — |
| `cReplaceId` | `size=8` | 8,765.0 ± 81.4 | — | 20,656.1 | — |
| `cSumPrices` | `size=512` | 348,970.9 ± 1,536.5 | — | 1,240,739.3 | — |
| `cSumPrices` | `size=64` | 43,446.0 ± 451.5 | — | 157,070.6 | — |
| `cSumPrices` | `size=8` | 6,340.3 ± 52.8 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 176.2 ± 5.4 | — | 0.1 | — |
| `jMiss` | `size=64` | 185.0 ± 2.9 | — | 0.0 | — |
| `jMiss` | `size=8` | 176.8 ± 6.3 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,238.2 ± 39.0 | — | 41,920.9 | — |
| `jModifyId` | `size=64` | 407.7 ± 3.2 | — | 5,352.0 | — |
| `jModifyId` | `size=8` | 106.7 ± 4.4 | — | 984.0 | — |
| `jReadId` | `size=512` | 36.5 ± 2.5 | — | 48.0 | — |
| `jReadId` | `size=64` | 38.1 ± 2.4 | — | 48.0 | — |
| `jReadId` | `size=8` | 38.9 ± 4.4 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 196.5 ± 2.1 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 197.8 ± 1.0 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 196.3 ± 1.4 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,191.5 ± 17.7 | — | 41,896.9 | — |
| `jReplaceId` | `size=64` | 396.7 ± 3.9 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 106.8 ± 2.7 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 86,443.0 ± 1,297.8 | — | 63,665.0 | — |
| `jSumPrices` | `size=64` | 10,439.7 ± 92.3 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,474.0 ± 52.9 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 65.0 ± 1.9 | — | 312.0 | — |
| `MapDrillModify` | `-` | 41.1 ± 0.9 | — | 216.0 | — |
| `MapGet` | `-` | 2.8 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 57.5 ± 4.7 | — | 200.0 | — |
| `handEnvUse` | `-` | 65.3 ± 0.7 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 35.5 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 54.6 ± 4.8 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.8 ± 0.0 | 4.3 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.9 ± 0.1 | 31.0 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.2 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,707.3 ± 928.0 | — | 43,035.8 | — |
| `Fold_powerEach` | `size=256` | 3,384.3 ± 30.7 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 402.5 ± 6.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 86.8 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 50,529.5 ± 535.7 | — | 331,099.0 | — |
| `Modify_multiFocus` | `size=256` | 11,443.6 ± 122.0 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,407.8 ± 16.7 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 232.3 ± 12.1 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,233.3 ± 332.9 | — | 115,177.1 | — |
| `Modify_powerEach` | `size=256` | 8,377.9 ± 22.9 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,042.4 ± 9.9 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 206.6 ± 11.5 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,060.7 ± 42.4 | — | 65,578.1 | — |
| `naive_listMap` | `size=256` | 2,036.4 ± 6.7 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 245.9 ± 0.9 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.0 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,275.0 ± 37.0 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 802.5 ± 37.7 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 67.8 ± 1.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 7.5 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 66.8 ± 0.3 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.1 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 173.4 ± 1.9 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 17.0 ± 0.0 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 28.5 ± 0.1 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 36.2 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.1 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.1 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 164.5 ± 0.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.2 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,125.1 ± 14.9 | — | 2,816.0 | — |
| `reuseUse` | `-` | 1,062.2 ± 12.0 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.8 ± 0.1 | 21.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 58.7 ± 0.7 | 71.9 ± 0.7 | 160.0 | 304.0 |
| `Modify_6` | `-` | 143.3 ± 0.7 | 120.9 ± 0.8 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.4 ± 0.1 | 16.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.6 ± 0.0 | 3.6 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.2 ± 0.0 | 7.1 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 34,904.2 ± 981.8 | — | 97,408.3 | — |
| `ModifyNames` | `size=64` | 4,334.5 ± 226.3 | — | 12,576.2 | — |
| `ModifyNames` | `size=8` | 598.0 ± 6.0 | — | 2,160.0 | — |
| `ModifyStreet` | `size=512` | 121.5 ± 0.7 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 120.9 ± 0.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 121.4 ± 0.3 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 38.3 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 38.2 ± 0.1 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 38.2 ± 0.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 101,109.9 ± 529.0 | — | 382,773.4 | — |
| `monocleModifyNames` | `size=64` | 9,898.0 ± 507.9 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,479.2 ± 10.4 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 55,743.7 ± 330.4 | — | 169,083.8 | — |
| `monocleModifyStreet` | `size=64` | 7,022.0 ± 567.9 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 986.8 ± 9.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 34,318.5 ± 222.7 | — | 69,791.1 | — |
| `monocleReadStreet` | `size=64` | 4,410.8 ± 28.6 | — | 8,848.2 | — |
| `monocleReadStreet` | `size=8` | 516.1 ± 1.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 68,964.0 ± 299.4 | — | 226,301.2 | — |
| `naiveModifyNames` | `size=64` | 8,471.8 ± 690.6 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,157.4 ± 5.7 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 55,648.1 ± 366.9 | — | 169,062.0 | — |
| `naiveModifyStreet` | `size=64` | 7,276.7 ± 23.9 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 994.9 ± 17.9 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 34,362.6 ± 202.6 | — | 69,790.3 | — |
| `naiveReadStreet` | `size=64` | 4,080.8 ± 508.2 | — | 8,840.1 | — |
| `naiveReadStreet` | `size=8` | 510.8 ± 1.4 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 252,746.4 ± 8,998.7 | — | 609,938.6 | — |
| `Names` | `size=64` | 32,080.4 ± 941.1 | — | 78,779.4 | — |
| `Names` | `size=8` | 4,525.0 ± 72.0 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 273,828.6 ± 9,544.1 | — | 683,563.4 | — |
| `NamesIor` | `size=64` | 35,178.0 ± 1,070.3 | — | 88,204.7 | — |
| `NamesIor` | `size=8` | 4,646.9 ± 76.9 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,083.8 ± 12.4 | — | 2,720.9 | — |
| `Street` | `size=64` | 1,074.9 ± 21.8 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,059.5 ± 18.1 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,079.1 ± 18.5 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,112.0 ± 26.1 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,118.5 ± 62.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 254,324.0 ± 6,784.1 | — | 614,003.8 | — |
| `directNames` | `size=64` | 31,086.5 ± 169.8 | — | 77,719.4 | — |
| `directNames` | `size=8` | 4,214.4 ± 46.0 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,066.7 ± 27.9 | — | 2,728.8 | — |
| `directStreet` | `size=64` | 1,075.1 ± 16.7 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,077.7 ± 6.1 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 251,173.5 ± 3,386.2 | — | 614,001.3 | — |
| `hcursorNames` | `size=64` | 30,880.7 ± 658.9 | — | 77,789.7 | — |
| `hcursorNames` | `size=8` | 4,226.1 ± 101.8 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,142.7 ± 15.9 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,157.2 ± 4.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,149.0 ± 24.4 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 232,783.6 ± 3,282.8 | — | 1,121,764.4 | — |
| `monocleNames` | `size=64` | 24,537.2 ± 749.2 | — | 132,409.2 | — |
| `monocleNames` | `size=8` | 3,835.1 ± 49.8 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 186,066.5 ± 2,963.1 | — | 908,029.0 | — |
| `monocleStreet` | `size=64` | 22,100.4 ± 203.3 | — | 113,804.6 | — |
| `monocleStreet` | `size=8` | 3,285.8 ± 25.8 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 198,900.4 ± 1,415.2 | — | 965,269.6 | — |
| `naiveNames` | `size=64` | 23,551.0 ± 28.5 | — | 120,842.1 | — |
| `naiveNames` | `size=8` | 3,475.3 ± 4.8 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 188,531.8 ± 2,719.7 | — | 908,030.7 | — |
| `naiveStreet` | `size=64` | 22,008.3 ± 168.8 | — | 113,791.3 | — |
| `naiveStreet` | `size=8` | 3,285.2 ± 12.6 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,596.2 ± 51.2 | — | 42,003.2 | — |
| `ModifyStreet` | `size=64` | 587.4 ± 2.6 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 298.4 ± 4.2 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 202.8 ± 1.8 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 197.2 ± 4.5 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 204.1 ± 2.8 | — | 90.7 | — |
| `SumPrices` | `size=512` | 86,733.6 ± 6,841.4 | — | 63,716.1 | — |
| `SumPrices` | `size=64` | 10,934.9 ± 322.3 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,445.8 ± 19.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 163,550.3 ± 2,197.4 | — | 333,584.6 | — |
| `monocleModifyStreet` | `size=64` | 19,861.5 ± 65.9 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,281.4 ± 24.2 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 93,315.1 ± 218.4 | — | 193,231.9 | — |
| `monocleReadStreet` | `size=64` | 12,748.9 ± 1,581.1 | — | 24,705.3 | — |
| `monocleReadStreet` | `size=8` | 1,907.3 ± 21.4 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 459,834.2 ± 27,837.8 | — | 1,190,779.8 | — |
| `monocleSumPrices` | `size=64` | 16,532.5 ± 48.0 | — | 47,377.7 | — |
| `monocleSumPrices` | `size=8` | 2,574.7 ± 18.0 | — | 6,640.1 | — |
| `naiveModifyStreet` | `size=512` | 162,821.0 ± 402.3 | — | 333,500.0 | — |
| `naiveModifyStreet` | `size=64` | 19,838.8 ± 86.4 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,313.1 ± 26.2 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 93,337.0 ± 1,439.6 | — | 193,231.9 | — |
| `naiveReadStreet` | `size=64` | 12,010.2 ± 255.1 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,886.8 ± 11.2 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 97,473.9 ± 322.6 | — | 230,123.4 | — |
| `naiveSumPrices` | `size=64` | 12,324.6 ± 59.8 | — | 29,337.3 | — |
| `naiveSumPrices` | `size=8` | 1,976.9 ± 6.7 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 34,607.2 ± 221.4 | — | 452.7 | — |
| `nativeReadStreet` | `size=64` | 4,418.7 ± 13.0 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 861.9 ± 20.7 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 63,439.9 ± 1,460.5 | — | 86,273.1 | — |
| `nativeSumPrices` | `size=64` | 7,925.7 ± 18.6 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,177.0 ± 4.6 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 123,302.1 ± 1,081.2 | — | 624,385.7 | — |
| `TransformDeep` | `n=512` | 11,667.9 ± 80.5 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,437.5 ± 3.8 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 130,056.3 ± 1,345.3 | 168,247.3 ± 817.4 | 655,358.6 | 753,738.4 |
| `TransformExpr` | `n=512` | 16,402.0 ± 109.8 | 15,473.1 ± 166.3 | 81,825.7 | 69,585.6 |
| `TransformExpr` | `n=64` | 2,028.5 ± 4.6 | 2,572.9 ± 5.3 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 103,519.4 ± 5,439.8 | — | 786,579.3 | — |
| `UniverseDeep` | `n=512` | 15,358.9 ± 45.9 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,892.7 ± 4.8 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 97,266.4 ± 1,275.9 | 2,712,196.0 ± 119,657.9 | 786,382.8 | 4,687,650.6 |
| `UniverseExpr` | `n=512` | 14,874.9 ± 61.3 | 182,862.0 ± 1,829.3 | 98,185.5 | 475,010.7 |
| `UniverseExpr` | `n=64` | 1,825.5 ± 6.1 | 15,482.8 ± 1,081.2 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 232,590.3 ± 1,852.9 | 2,893,478.4 ± 143,162.5 | 786,481.3 | 6,882,814.2 |
| `UniverseJson` | `n=512` | 27,436.3 ± 180.1 | 203,679.7 ± 3,496.4 | 98,186.8 | 699,916.8 |
| `UniverseJson` | `n=64` | 3,327.0 ± 4.6 | 19,536.2 ± 388.4 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 41,544.8 ± 125.3 | — | 163,886.2 | — |
| `visitorTransformDeep` | `n=512` | 4,902.7 ± 27.0 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 429.1 ± 4.3 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 63,926.0 ± 234.4 | — | 360,470.5 | — |
| `visitorTransformExpr` | `n=512` | 7,594.4 ± 87.3 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 989.6 ± 10.2 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 58,620.0 ± 706.0 | — | 196,706.7 | — |
| `visitorUniverseDeep` | `n=512` | 7,239.7 ± 23.4 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 829.3 ± 3.6 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 53,863.7 ± 622.3 | — | 196,655.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,726.1 ± 25.2 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 817.4 ± 1.4 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 193,820.7 ± 4,250.3 | — | 469,789.1 | — |
| `visitorUniverseJson` | `n=512` | 21,187.0 ± 74.6 | — | 40,954.2 | — |
| `visitorUniverseJson` | `n=64` | 2,052.7 ± 81.1 | — | 4,760.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,524.8 ± 119.9 | — | 41,413.9 | — |
| `Modify_powerEach` | `size=16` | 264.5 ± 1.0 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,082.7 ± 8.9 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 125.5 ± 0.5 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 53,110.3 ± 1,637.5 | — | 164,374.5 | — |
| `Modify_powerEach` | `size=64` | 825.4 ± 10.0 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 59,147.0 ± 1,472.4 | — | 279,432.2 | — |
| `monocle_powerEach` | `size=16` | 631.9 ± 3.4 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,822.8 ± 111.1 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 264.1 ± 4.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 184,170.5 ± 1,041.0 | — | 967,851.9 | — |
| `monocle_powerEach` | `size=64` | 2,089.2 ± 24.3 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,086.2 ± 12.3 | — | 28,730.4 | — |
| `naive_powerEach` | `size=16` | 99.3 ± 0.7 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,613.1 ± 1.9 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.2 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 20,239.8 ± 187.7 | — | 114,777.0 | — |
| `naive_powerEach` | `size=64` | 398.2 ± 2.5 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 69,506.1 ± 6,809.7 | — | 210,678.5 | — |
| `Modify_nested` | `size=16` | 1,498.8 ± 12.1 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,964.0 ± 72.6 | — | 53,824.7 | — |
| `Modify_nested` | `size=4` | 700.0 ± 6.6 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,756.5 ± 37.0 | — | 14,664.5 | — |
| `monocle_nested` | `size=1024` | 248,218.6 ± 2,769.4 | — | 1,118,851.9 | — |
| `monocle_nested` | `size=16` | 2,857.6 ± 26.0 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 93,209.9 ± 1,377.9 | — | 430,210.4 | — |
| `monocle_nested` | `size=4` | 1,381.8 ± 101.2 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,664.8 ± 75.4 | — | 58,896.9 | — |
| `naive_nested` | `size=1024` | 19,433.1 ± 187.3 | — | 115,067.4 | — |
| `naive_nested` | `size=16` | 367.6 ± 4.1 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,211.1 ± 24.0 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 127.8 ± 2.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,328.1 ± 26.4 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,348.9 ± 6.0 | — | 4,840.1 | — |
| `Modify_sparse` | `size=2048` | 22,995.6 ± 438.8 | — | 104,699.0 | — |
| `Modify_sparse` | `size=32` | 356.5 ± 3.7 | — | 1,384.0 | — |
| `Modify_sparse` | `size=512` | 5,731.0 ± 15.7 | — | 24,809.6 | — |
| `Modify_sparse` | `size=8` | 128.9 ± 1.5 | — | 520.0 | — |
| `monocle_sparse` | `size=128` | 3,820.9 ± 11.9 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 106,646.6 ± 559.9 | — | 523,147.8 | — |
| `monocle_sparse` | `size=32` | 1,085.2 ± 69.4 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 33,482.8 ± 537.6 | — | 166,684.7 | — |
| `monocle_sparse` | `size=8` | 332.2 ± 3.7 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 324.7 ± 3.3 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,987.8 ± 21.7 | — | 24,612.3 | — |
| `naive_sparse` | `size=32` | 83.2 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,162.7 ± 3.6 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.2 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.5 ± 0.1 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.6 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 20.0 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.4 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.6 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 105,319.3 ± 951.9 | — | 589,712.7 | — |
| `Cata` | `-` | 78,883.6 ± 874.2 | — | 197,568.6 | — |
| `Hylo` | `-` | 85,866.3 ± 542.6 | — | 295,848.6 | — |
| `drosteAna` | `-` | 47,023.8 ± 549.0 | — | 327,632.3 | — |
| `drosteCata` | `-` | 39,260.9 ± 228.8 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 64,585.0 ± 143.7 | — | 328,640.5 | — |
| `handAna` | `-` | 19,312.9 ± 324.3 | — | 163,816.1 | — |
| `handCata` | `-` | 13,014.2 ± 34.5 | — | 0.1 | — |
| `handHylo` | `-` | 9,951.5 ± 655.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.3 ± 0.0 | 2.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.7 ± 0.0 | 28.2 ± 0.4 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.4 ± 0.1 | 64.6 ± 0.3 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.1 ± 0.0 | 3.1 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,176.3 ± 143.0 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 529.3 ± 22.1 | — | 2,648.0 | — |
| `FoldNested` | `size=8` | 51.2 ± 0.7 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,798.5 ± 11.1 | 31,882.7 ± 449.0 | 12,312.4 | 162,581.1 |
| `FoldPrices` | `size=64` | 351.6 ± 3.7 | 2,170.2 ± 11.6 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 44.3 ± 0.1 | 342.5 ± 2.1 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 7,418.2 ± 132.9 | 33,118.9 ± 233.4 | 36,897.2 | 176,925.1 |
| `Modify` | `size=64` | 861.3 ± 1.6 | 1,695.0 ± 9.0 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 96.6 ± 0.2 | 288.7 ± 4.3 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 12.0 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 113.8 ± 0.5 | — | 528.0 | — |
| `ServiceGet` | `-` | 5.1 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 81.6 ± 0.5 | — | 456.0 | — |
| `handDrillGet` | `-` | 14.7 ± 0.2 | — | 120.0 | — |
| `handDrillModify` | `-` | 106.1 ± 1.3 | — | 648.0 | — |
| `handServiceGet` | `-` | 14.7 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 91.4 ± 3.9 | — | 576.0 | — |


# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `11b40654e6045a27633a8b1e1db6203869476004` · date: `2026-09-23` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json` · profile: `sweep:-i5-wi3-f3-t1-gc`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.5 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.5 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 8.0 ± 0.3 | 5.0 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 16.7 ± 0.2 | 13.9 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 79.2 ± 1.3 | — | 720.0 | — |
| `ModifyCountry` | `-` | 360.6 ± 14.7 | — | 3,205.3 | — |
| `ModifyPartner` | `-` | 382.3 ± 6.7 | — | 3,256.0 | — |
| `ReadCountry` | `-` | 115.3 ± 9.3 | — | 520.0 | — |
| `ReadPartner` | `-` | 122.0 ± 4.4 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 196.5 ± 4.5 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 1,613.2 ± 39.0 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 1,767.0 ± 56.6 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,009.8 ± 53.4 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 952.9 ± 39.4 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,226.8 ± 37.0 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 520.6 ± 18.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 343.8 ± 5.6 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 210.9 ± 1.2 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 240.8 ± 4.6 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 836.5 ± 24.5 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 769.4 ± 33.4 | — | 3,344.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,156.6 ± 17.0 | — | 9,400.0 | — |
| `ClickToJson` | `-` | 1,812.3 ± 37.6 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 731.2 ± 33.7 | — | 6,594.7 | — |
| `WideToJson` | `-` | 341.5 ± 17.0 | — | 1,482.7 | — |
| `naiveClickToAvro` | `-` | 988.6 ± 34.4 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 1,862.1 ± 93.1 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 627.4 ± 9.9 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,108.7 ± 44.6 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 120.2 ± 2.1 | — | 880.0 | — |
| `decode_native` | `-` | 9.9 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 111.3 ± 2.3 | — | 880.0 | — |
| `encode_bridged` | `-` | 128.5 ± 3.3 | — | 1,229.3 | — |
| `encode_native` | `-` | 7.9 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 128.0 ± 4.6 | — | 1,240.0 | — |
| `fieldGet_bridged` | `-` | 57.7 ± 2.3 | — | 432.0 | — |
| `fieldGet_native` | `-` | 58.1 ± 2.3 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 202.3 ± 5.4 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 93.9 ± 1.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 12.8 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 14.2 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 13.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 13.3 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 2.2 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 18.0 ± 0.3 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 19.8 ± 1.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 2.8 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 2.5 ± 0.2 | — | 40.0 | — |
| `modifyDirect` | `-` | 2.0 ± 0.1 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 276.1 ± 13.2 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 279.6 ± 7.6 | — | 744.0 | — |
| `encode_hand` | `-` | 81.1 ± 1.9 | — | 768.0 | — |
| `encode_positional` | `-` | 3,136.8 ± 104.5 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 3,823.2 ± 118.4 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 2.6 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 7.8 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 14.5 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 7.9 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 1.5 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 9.4 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 25.7 ± 0.7 | — | 72.0 | — |
| `reuseLens6` | `-` | 77.6 ± 1.9 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 33.0 ± 0.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 2,250.2 ± 80.9 | 2,191.8 ± 97.5 | 14,080.4 | 14,080.4 |
| `FoldMap` | `size=64` | 201.2 ± 3.8 | 201.3 ± 3.9 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 12.6 ± 0.2 | 12.5 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 1,778.5 ± 129.1 | 1,773.3 ± 27.9 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 176.4 ± 0.8 | 180.6 ± 4.4 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 21.3 ± 0.2 | 21.5 ± 0.4 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.2 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 1.7 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 1.5 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 1.6 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.3 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 1.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 8.8 ± 0.1 | 4.4 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 17.7 ± 0.3 | 11.9 ± 0.1 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.4 ± 0.0 | 0.2 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.9 ± 0.0 | 2.0 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 1.5 ± 0.0 | 1.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 213,321.8 ± 2,833.2 | — | 1,066,660.7 | — |
| `cModifyId` | `size=64` | 28,272.1 ± 410.0 | — | 136,259.0 | — |
| `cModifyId` | `size=8` | 4,656.3 ± 110.6 | — | 20,704.0 | — |
| `cReadId` | `size=512` | 104,024.1 ± 1,241.9 | — | 797,913.9 | — |
| `cReadId` | `size=64` | 13,306.7 ± 177.7 | — | 101,288.4 | — |
| `cReadId` | `size=8` | 2,090.5 ± 37.5 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 107,345.8 ± 2,436.4 | — | 797,912.9 | — |
| `cReadStreet` | `size=64` | 13,592.7 ± 434.2 | — | 101,288.4 | — |
| `cReadStreet` | `size=8` | 2,101.2 ± 34.7 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 216,065.1 ± 3,267.5 | — | 1,066,621.4 | — |
| `cReplaceId` | `size=64` | 28,138.1 ± 597.6 | — | 136,202.9 | — |
| `cReplaceId` | `size=8` | 4,738.7 ± 103.5 | — | 20,648.0 | — |
| `cSumPrices` | `size=512` | 187,881.9 ± 3,427.6 | — | 1,240,693.4 | — |
| `cSumPrices` | `size=64` | 23,517.6 ± 737.2 | — | 157,752.7 | — |
| `cSumPrices` | `size=8` | 3,270.0 ± 73.0 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 99.3 ± 2.5 | — | 0.0 | — |
| `jMiss` | `size=64` | 98.0 ± 2.0 | — | 0.0 | — |
| `jMiss` | `size=8` | 98.0 ± 1.8 | — | 0.0 | — |
| `jModifyId` | `size=512` | 1,792.2 ± 50.0 | — | 41,928.5 | — |
| `jModifyId` | `size=64` | 251.4 ± 9.5 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 64.1 ± 0.8 | — | 992.0 | — |
| `jReadId` | `size=512` | 21.6 ± 0.3 | — | 72.0 | — |
| `jReadId` | `size=64` | 23.5 ± 2.9 | — | 64.0 | — |
| `jReadId` | `size=8` | 22.7 ± 3.3 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 114.9 ± 3.3 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 115.1 ± 1.8 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 117.1 ± 2.2 | — | 152.0 | — |
| `jReplaceId` | `size=512` | 1,731.8 ± 42.3 | — | 41,888.5 | — |
| `jReplaceId` | `size=64` | 242.6 ± 2.8 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 61.4 ± 0.5 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 49,397.2 ± 2,793.3 | — | 63,685.0 | — |
| `jSumPrices` | `size=64` | 6,119.0 ± 131.4 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 788.1 ± 39.7 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 34.0 ± 5.2 | — | 312.0 | — |
| `MapDrillModify` | `-` | 21.9 ± 0.4 | — | 216.0 | — |
| `MapGet` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 31.2 ± 4.7 | — | 200.0 | — |
| `handEnvUse` | `-` | 39.4 ± 0.4 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 19.5 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 26.0 ± 5.7 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.5 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 2.0 ± 0.1 | 2.3 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 19.3 ± 0.2 | 19.1 ± 0.8 | 152.0 | 176.0 |
| `Replace` | `-` | 1.6 ± 0.0 | 1.7 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 9,924.9 ± 1,326.0 | — | 43,034.6 | — |
| `Fold_powerEach` | `size=256` | 2,094.3 ± 68.8 | — | 9,240.1 | — |
| `Fold_powerEach` | `size=32` | 229.0 ± 2.3 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 38.5 ± 1.1 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 33,260.8 ± 681.2 | — | 331,112.3 | — |
| `Modify_multiFocus` | `size=256` | 7,511.5 ± 196.8 | — | 76,112.5 | — |
| `Modify_multiFocus` | `size=32` | 935.4 ± 52.1 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 114.7 ± 2.4 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 22,129.5 ± 603.5 | — | 115,173.7 | — |
| `Modify_powerEach` | `size=256` | 5,261.7 ± 75.8 | — | 26,080.4 | — |
| `Modify_powerEach` | `size=32` | 697.8 ± 21.0 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 100.8 ± 2.2 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 5,786.9 ± 314.2 | — | 65,577.5 | — |
| `naive_listMap` | `size=256` | 1,296.6 ± 41.5 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 132.2 ± 5.3 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 17.5 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 2,942.2 ± 74.5 | — | 16,128.8 | — |
| `naive_sumQty` | `size=256` | 357.6 ± 8.7 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 39.7 ± 0.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 4.5 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 35.1 ± 0.7 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 95.6 ± 1.6 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.8 ± 0.2 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 13.8 ± 0.3 | — | 224.0 | — |
| `naive_constSum` | `-` | 0.9 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 24.3 ± 0.5 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 3.9 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 7.3 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 90.4 ± 1.8 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 25.7 ± 0.7 | — | 184.0 | — |
| `buildAndUse` | `-` | 565.1 ± 13.3 | — | 2,888.0 | — |
| `reuseUse` | `-` | 530.3 ± 13.0 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 12.7 ± 0.1 | 13.2 ± 0.3 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 0.6 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 33.1 ± 0.9 | 36.5 ± 0.8 | 160.0 | 304.0 |
| `Modify_6` | `-` | 79.7 ± 2.2 | 58.1 ± 1.8 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 8.7 ± 0.1 | 8.6 ± 0.2 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 0.6 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 2.4 ± 0.0 | 1.6 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 3.9 ± 0.1 | 3.3 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 21,824.3 ± 438.1 | — | 97,405.2 | — |
| `ModifyNames` | `size=64` | 2,612.0 ± 76.1 | — | 12,584.1 | — |
| `ModifyNames` | `size=8` | 358.6 ± 10.0 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 75.9 ± 1.4 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 77.6 ± 1.9 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 79.5 ± 1.6 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 23.2 ± 1.0 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 23.3 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 24.1 ± 1.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 59,729.1 ± 911.3 | — | 382,781.9 | — |
| `monocleModifyNames` | `size=64` | 5,549.5 ± 328.6 | — | 39,840.2 | — |
| `monocleModifyNames` | `size=8` | 844.0 ± 29.7 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 31,844.0 ± 484.4 | — | 169,068.0 | — |
| `monocleModifyStreet` | `size=64` | 3,807.5 ± 268.9 | — | 20,896.1 | — |
| `monocleModifyStreet` | `size=8` | 522.1 ± 7.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 19,648.4 ± 788.6 | — | 69,780.7 | — |
| `monocleReadStreet` | `size=64` | 2,445.1 ± 57.7 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 278.3 ± 3.1 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 41,296.0 ± 1,007.1 | — | 226,284.6 | — |
| `naiveModifyNames` | `size=64` | 5,056.0 ± 117.2 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 704.0 ± 13.8 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 32,046.9 ± 841.5 | — | 169,044.2 | — |
| `naiveModifyStreet` | `size=64` | 3,636.8 ± 258.5 | — | 20,864.1 | — |
| `naiveModifyStreet` | `size=8` | 520.6 ± 14.2 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 19,004.5 ± 123.8 | — | 69,780.5 | — |
| `naiveReadStreet` | `size=64` | 2,440.4 ± 39.4 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 276.9 ± 3.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 117,572.8 ± 5,321.2 | — | 609,830.3 | — |
| `Names` | `size=64` | 14,628.5 ± 344.2 | — | 78,257.5 | — |
| `Names` | `size=8` | 2,060.1 ± 80.7 | — | 10,872.0 | — |
| `NamesIor` | `size=512` | 126,123.0 ± 4,987.3 | — | 675,238.0 | — |
| `NamesIor` | `size=64` | 15,988.1 ± 627.6 | — | 86,489.7 | — |
| `NamesIor` | `size=8` | 2,124.1 ± 50.7 | — | 11,592.0 | — |
| `Street` | `size=512` | 515.7 ± 9.3 | — | 2,720.4 | — |
| `Street` | `size=64` | 517.0 ± 12.4 | — | 2,720.1 | — |
| `Street` | `size=8` | 512.2 ± 8.1 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 518.2 ± 3.8 | — | 2,736.4 | — |
| `StreetIor` | `size=64` | 523.3 ± 9.0 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 518.2 ± 8.3 | — | 2,736.0 | — |
| `directNames` | `size=512` | 119,630.0 ± 3,172.9 | — | 609,798.9 | — |
| `directNames` | `size=64` | 15,727.0 ± 781.2 | — | 77,713.7 | — |
| `directNames` | `size=8` | 1,952.1 ± 72.8 | — | 10,616.0 | — |
| `directStreet` | `size=512` | 542.0 ± 20.5 | — | 2,744.4 | — |
| `directStreet` | `size=64` | 530.7 ± 7.8 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 535.9 ± 27.0 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 125,598.4 ± 5,341.6 | — | 613,907.2 | — |
| `hcursorNames` | `size=64` | 15,161.1 ± 314.7 | — | 76,737.6 | — |
| `hcursorNames` | `size=8` | 2,006.0 ± 63.1 | — | 10,696.0 | — |
| `hcursorStreet` | `size=512` | 599.1 ± 9.8 | — | 3,032.5 | — |
| `hcursorStreet` | `size=64` | 604.2 ± 23.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 583.0 ± 14.4 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 129,897.5 ± 2,849.4 | — | 1,121,700.6 | — |
| `monocleNames` | `size=64` | 14,903.3 ± 198.8 | — | 132,750.7 | — |
| `monocleNames` | `size=8` | 2,217.0 ± 39.1 | — | 19,504.0 | — |
| `monocleStreet` | `size=512` | 100,218.1 ± 2,028.4 | — | 907,989.3 | — |
| `monocleStreet` | `size=64` | 13,160.7 ± 178.4 | — | 113,798.5 | — |
| `monocleStreet` | `size=8` | 1,886.5 ± 34.7 | — | 17,048.0 | — |
| `naiveNames` | `size=512` | 109,566.9 ± 1,655.4 | — | 965,215.9 | — |
| `naiveNames` | `size=64` | 14,277.9 ± 222.4 | — | 120,835.9 | — |
| `naiveNames` | `size=8` | 1,966.4 ± 32.2 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 96,637.6 ± 2,688.0 | — | 907,980.4 | — |
| `naiveStreet` | `size=64` | 13,011.7 ± 638.9 | — | 113,785.2 | — |
| `naiveStreet` | `size=8` | 1,787.4 ± 48.8 | — | 17,045.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 1,885.8 ± 46.1 | — | 42,025.7 | — |
| `ModifyStreet` | `size=64` | 357.2 ± 10.1 | — | 5,432.0 | — |
| `ModifyStreet` | `size=8` | 171.1 ± 6.8 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 116.9 ± 2.3 | — | 128.1 | — |
| `ReadStreet` | `size=64` | 112.0 ± 3.9 | — | 90.7 | — |
| `ReadStreet` | `size=8` | 110.2 ± 2.3 | — | 72.0 | — |
| `SumPrices` | `size=512` | 43,605.0 ± 675.4 | — | 63,710.8 | — |
| `SumPrices` | `size=64` | 5,926.7 ± 237.3 | — | 8,120.7 | — |
| `SumPrices` | `size=8` | 759.9 ± 19.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 92,849.8 ± 1,739.4 | — | 333,520.2 | — |
| `monocleModifyStreet` | `size=64` | 11,665.1 ± 353.5 | — | 30,081.2 | — |
| `monocleModifyStreet` | `size=8` | 2,014.9 ± 18.2 | — | 4,664.0 | — |
| `monocleReadStreet` | `size=512` | 52,100.9 ± 755.9 | — | 193,196.6 | — |
| `monocleReadStreet` | `size=64` | 6,644.2 ± 152.9 | — | 24,704.7 | — |
| `monocleReadStreet` | `size=8` | 1,087.3 ± 70.2 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 230,988.6 ± 8,325.8 | — | 1,190,581.7 | — |
| `monocleSumPrices` | `size=64` | 9,304.1 ± 134.9 | — | 47,385.0 | — |
| `monocleSumPrices` | `size=8` | 1,513.3 ± 39.6 | — | 6,624.0 | — |
| `naiveModifyStreet` | `size=512` | 95,492.2 ± 2,241.6 | — | 333,490.7 | — |
| `naiveModifyStreet` | `size=64` | 12,061.1 ± 253.6 | — | 30,057.2 | — |
| `naiveModifyStreet` | `size=8` | 2,066.2 ± 64.0 | — | 4,640.0 | — |
| `naiveReadStreet` | `size=512` | 55,308.9 ± 1,510.6 | — | 193,199.3 | — |
| `naiveReadStreet` | `size=64` | 6,764.2 ± 171.7 | — | 24,704.7 | — |
| `naiveReadStreet` | `size=8` | 1,054.6 ± 19.2 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 57,368.2 ± 823.1 | — | 230,089.1 | — |
| `naiveSumPrices` | `size=64` | 7,185.9 ± 101.9 | — | 29,336.7 | — |
| `naiveSumPrices` | `size=8` | 1,119.8 ± 20.1 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 21,178.3 ± 336.3 | — | 441.7 | — |
| `nativeReadStreet` | `size=64` | 2,751.8 ± 30.5 | — | 424.3 | — |
| `nativeReadStreet` | `size=8` | 495.0 ± 21.6 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 36,621.9 ± 660.7 | — | 86,262.4 | — |
| `nativeSumPrices` | `size=64` | 4,571.1 ± 95.3 | — | 10,920.5 | — |
| `nativeSumPrices` | `size=8` | 689.2 ± 14.6 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 72,096.3 ± 1,501.9 | — | 624,348.5 | — |
| `TransformDeep` | `n=512` | 8,753.9 ± 155.2 | — | 57,360.9 | — |
| `TransformDeep` | `n=64` | 1,046.4 ± 15.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 78,550.6 ± 3,335.1 | 96,453.4 ± 2,006.8 | 655,321.2 | 753,686.2 |
| `TransformExpr` | `n=512` | 9,966.1 ± 212.4 | 8,472.9 ± 150.2 | 81,825.0 | 69,584.9 |
| `TransformExpr` | `n=64` | 1,207.5 ± 33.9 | 1,460.0 ± 35.6 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 70,163.5 ± 1,962.5 | — | 786,555.1 | — |
| `UniverseDeep` | `n=512` | 9,118.4 ± 71.1 | — | 98,376.9 | — |
| `UniverseDeep` | `n=64` | 1,098.3 ± 13.1 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 71,001.3 ± 2,809.1 | 2,736,404.8 ± 159,796.6 | 786,363.7 | 4,687,668.3 |
| `UniverseExpr` | `n=512` | 8,744.8 ± 127.0 | 217,409.7 ± 6,805.3 | 98,184.9 | 475,014.3 |
| `UniverseExpr` | `n=64` | 1,045.6 ± 29.5 | 18,881.1 ± 556.9 | 12,168.0 | 45,424.4 |
| `UniverseJson` | `n=4096` | 146,363.5 ± 2,526.4 | 2,828,167.3 ± 118,265.3 | 786,418.5 | 6,489,598.1 |
| `UniverseJson` | `n=512` | 16,019.9 ± 438.4 | 223,049.6 ± 4,934.3 | 98,185.6 | 699,918.8 |
| `UniverseJson` | `n=64` | 1,946.4 ± 50.9 | 20,889.2 ± 291.8 | 12,168.0 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 22,879.6 ± 1,187.4 | — | 163,872.7 | — |
| `visitorTransformDeep` | `n=512` | 2,522.3 ± 85.0 | — | 20,496.3 | — |
| `visitorTransformDeep` | `n=64` | 240.3 ± 3.5 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 38,978.9 ± 1,289.3 | — | 360,452.4 | — |
| `visitorTransformExpr` | `n=512` | 4,730.0 ± 77.8 | — | 45,032.5 | — |
| `visitorTransformExpr` | `n=64` | 621.2 ± 18.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 38,323.4 ± 1,426.4 | — | 196,691.9 | — |
| `visitorUniverseDeep` | `n=512` | 4,760.3 ± 100.8 | — | 24,632.5 | — |
| `visitorUniverseDeep` | `n=64` | 462.8 ± 12.9 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 33,072.3 ± 1,146.8 | — | 196,640.1 | — |
| `visitorUniverseExpr` | `n=512` | 3,850.3 ± 48.5 | — | 24,584.4 | — |
| `visitorUniverseExpr` | `n=64` | 442.5 ± 17.4 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 112,099.4 ± 4,809.4 | — | 527,351.4 | — |
| `visitorUniverseJson` | `n=512` | 12,298.8 ± 290.6 | — | 40,953.3 | — |
| `visitorUniverseJson` | `n=64` | 1,125.6 ± 44.4 | — | 5,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 8,117.6 ± 188.1 | — | 41,411.8 | — |
| `Modify_powerEach` | `size=16` | 141.0 ± 2.9 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,075.4 ± 39.7 | — | 10,688.3 | — |
| `Modify_powerEach` | `size=4` | 60.5 ± 2.4 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 31,378.2 ± 705.4 | — | 164,339.1 | — |
| `Modify_powerEach` | `size=64` | 519.5 ± 19.4 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 30,177.1 ± 545.5 | — | 279,407.6 | — |
| `monocle_powerEach` | `size=16` | 420.5 ± 7.2 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 12,058.0 ± 406.1 | — | 107,329.9 | — |
| `monocle_powerEach` | `size=4` | 130.6 ± 2.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 102,481.8 ± 1,776.4 | — | 967,726.5 | — |
| `monocle_powerEach` | `size=64` | 1,308.7 ± 19.9 | — | 14,520.0 | — |
| `naive_powerEach` | `size=1024` | 3,868.8 ± 123.3 | — | 28,729.8 | — |
| `naive_powerEach` | `size=16` | 46.5 ± 1.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 769.0 ± 29.1 | — | 7,224.1 | — |
| `naive_powerEach` | `size=4` | 14.5 ± 0.4 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 12,937.2 ± 301.1 | — | 114,765.1 | — |
| `naive_powerEach` | `size=64` | 172.7 ± 1.4 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 39,515.9 ± 3,305.1 | — | 210,546.6 | — |
| `Modify_nested` | `size=16` | 942.8 ± 30.5 | — | 4,792.0 | — |
| `Modify_nested` | `size=256` | 11,062.4 ± 317.9 | — | 53,727.0 | — |
| `Modify_nested` | `size=4` | 376.0 ± 6.5 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 2,924.9 ± 95.5 | — | 14,592.3 | — |
| `monocle_nested` | `size=1024` | 121,061.1 ± 1,713.3 | — | 1,118,621.9 | — |
| `monocle_nested` | `size=16` | 1,674.6 ± 77.8 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 49,713.0 ± 862.9 | — | 430,185.7 | — |
| `monocle_nested` | `size=4` | 783.8 ± 91.8 | — | 5,546.7 | — |
| `monocle_nested` | `size=64` | 4,713.7 ± 104.9 | — | 58,912.5 | — |
| `naive_nested` | `size=1024` | 13,128.7 ± 766.0 | — | 115,055.9 | — |
| `naive_nested` | `size=16` | 207.5 ± 5.3 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 3,611.6 ± 76.4 | — | 29,018.3 | — |
| `naive_nested` | `size=4` | 73.5 ± 1.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 863.3 ± 68.6 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 822.7 ± 15.9 | — | 4,816.0 | — |
| `Modify_sparse` | `size=2048` | 13,813.8 ± 247.1 | — | 104,667.1 | — |
| `Modify_sparse` | `size=32` | 192.9 ± 6.9 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 3,474.2 ± 80.1 | — | 24,785.0 | — |
| `Modify_sparse` | `size=8` | 63.1 ± 0.9 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 2,551.5 ± 88.6 | — | 27,808.1 | — |
| `monocle_sparse` | `size=2048` | 52,511.4 ± 1,813.1 | — | 523,110.8 | — |
| `monocle_sparse` | `size=32` | 783.4 ± 65.5 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 17,244.3 ± 290.6 | — | 166,676.2 | — |
| `monocle_sparse` | `size=8` | 196.6 ± 6.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 167.8 ± 3.1 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 3,328.7 ± 64.3 | — | 24,610.8 | — |
| `naive_sparse` | `size=32` | 43.1 ± 1.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 754.7 ± 20.1 | — | 6,176.2 | — |
| `naive_sparse` | `size=8` | 14.0 ± 0.7 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.5 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.5 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 1.2 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 10.3 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 17.9 ± 0.5 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.0 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 2.7 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 4.4 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 53,468.0 ± 1,071.4 | — | 589,712.4 | — |
| `Cata` | `-` | 44,330.2 ± 452.0 | — | 197,568.3 | — |
| `Hylo` | `-` | 46,823.4 ± 663.1 | — | 295,848.3 | — |
| `drosteAna` | `-` | 23,624.8 ± 425.0 | — | 327,632.2 | — |
| `drosteCata` | `-` | 24,318.6 ± 439.1 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 28,160.6 ± 616.0 | — | 328,640.2 | — |
| `handAna` | `-` | 10,573.0 ± 133.6 | — | 163,816.1 | — |
| `handCata` | `-` | 7,347.2 ± 75.9 | — | 0.1 | — |
| `handHylo` | `-` | 6,075.1 ± 52.6 | — | 0.0 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.3 ± 0.0 | 1.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 7.0 ± 0.1 | 14.4 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 15.3 ± 0.3 | 30.9 ± 1.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 1.6 ± 0.0 | 1.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 3,359.8 ± 58.1 | — | 20,200.5 | — |
| `FoldNested` | `size=64` | 307.1 ± 7.1 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 32.0 ± 0.8 | — | 424.0 | — |
| `FoldPrices` | `size=512` | 1,842.7 ± 61.1 | 16,541.3 ± 285.4 | 12,312.3 | 162,578.6 |
| `FoldPrices` | `size=64` | 181.8 ± 4.7 | 1,432.4 ± 33.3 | 1,560.0 | 15,424.0 |
| `FoldPrices` | `size=8` | 22.0 ± 0.5 | 184.6 ± 3.7 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 5,258.4 ± 344.7 | 18,271.7 ± 394.0 | 36,896.8 | 176,922.9 |
| `Modify` | `size=64` | 719.0 ± 32.3 | 1,179.9 ± 62.4 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 52.4 ± 1.1 | 170.9 ± 7.0 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 6.4 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 75.2 ± 1.8 | — | 528.0 | — |
| `ServiceGet` | `-` | 2.8 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 59.1 ± 1.3 | — | 456.0 | — |
| `handDrillGet` | `-` | 8.2 ± 0.3 | — | 120.0 | — |
| `handDrillModify` | `-` | 70.4 ± 1.7 | — | 648.0 | — |
| `handServiceGet` | `-` | 8.9 ± 0.6 | — | 120.0 | — |
| `handServiceReplace` | `-` | 64.3 ± 1.1 | — | 576.0 | — |


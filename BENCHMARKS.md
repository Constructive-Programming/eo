# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `96bc4e2372f9968afd51764901fe0e3d0350d324` · date: `2026-09-30` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.8 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 13.4 ± 0.5 | 9.3 ± 0.2 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 25.6 ± 0.3 | 24.9 ± 0.6 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 157.6 ± 2.5 | — | 720.0 | — |
| `ModifyCountry` | `-` | 319.8 ± 5.6 | — | 928.0 | — |
| `ModifyPartner` | `-` | 403.5 ± 5.4 | — | 994.7 | — |
| `ReadCountry` | `-` | 178.3 ± 5.7 | — | 520.0 | — |
| `ReadPartner` | `-` | 213.9 ± 2.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 310.6 ± 2.7 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,735.0 ± 16.7 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 3,104.9 ± 45.7 | — | 8,696.1 | — |
| `naivePassthroughPayload` | `-` | 5,224.1 ± 117.9 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,686.5 ± 16.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,058.4 ± 12.7 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 906.9 ± 3.0 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 671.5 ± 3.7 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 371.1 ± 1.6 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 422.1 ± 2.1 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,515.0 ± 12.7 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,453.9 ± 16.5 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 57.2 ± 0.2 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 648.4 ± 6.3 | — | 562.7 | — |
| `handwritten_stream` | `-` | 169.8 ± 8.1 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 762.2 ± 13.5 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,062.8 ± 131.5 | — | 4,653.4 | — |
| `ClickToJson` | `-` | 2,844.3 ± 34.8 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 855.7 ± 9.3 | — | 2,040.0 | — |
| `WideToJson` | `-` | 646.7 ± 9.6 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,549.2 ± 9.4 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 3,096.5 ± 53.6 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 1,055.9 ± 8.0 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,921.1 ± 38.4 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 232.6 ± 1.9 | — | 880.0 | — |
| `decode_native` | `-` | 16.9 ± 0.3 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 228.0 ± 1.4 | — | 880.0 | — |
| `encode_bridged` | `-` | 251.8 ± 2.0 | — | 1,229.3 | — |
| `encode_native` | `-` | 12.3 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 252.9 ± 12.6 | — | 1,218.7 | — |
| `fieldGet_bridged` | `-` | 100.8 ± 0.9 | — | 432.0 | — |
| `fieldGet_native` | `-` | 100.6 ± 0.6 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 432.5 ± 3.5 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 178.7 ± 2.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 17.8 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 18.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 17.7 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 17.7 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 5.1 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.3 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 36.3 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.9 ± 0.2 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.6 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.0 ± 0.0 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 490.9 ± 3.9 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 492.9 ± 5.3 | — | 744.0 | — |
| `encode_hand` | `-` | 173.2 ± 0.5 | — | 768.0 | — |
| `encode_positional` | `-` | 5,460.4 ± 77.2 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 6,445.6 ± 92.8 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 9.6 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 27.7 ± 0.3 | — | 184.0 | — |
| `buildLens6` | `-` | 55.7 ± 0.7 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 28.9 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.0 ± 0.3 | — | 40.0 | — |
| `reuseLens3` | `-` | 41.1 ± 1.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 111.2 ± 1.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 53.4 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,697.7 ± 254.7 | 3,474.2 ± 64.2 | 14,080.6 | 14,080.6 |
| `FoldMap` | `size=64` | 313.2 ± 1.8 | 300.3 ± 5.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 17.6 ± 0.2 | 17.4 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 3,274.8 ± 19.6 | 3,166.2 ± 18.2 | 12,312.5 | 12,312.5 |
| `FoldPrices` | `size=64` | 324.0 ± 1.8 | 309.0 ± 2.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 44.3 ± 0.7 | 43.2 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 4.1 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.5 ± 0.1 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.6 ± 0.5 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 1.0 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 14.1 ± 0.2 | 8.1 ± 0.5 | 0.0 | 0.0 |
| `Get_6` | `-` | 27.9 ± 1.5 | 23.3 ± 1.2 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 1.0 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.7 ± 0.4 | 4.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 4.0 ± 0.1 | 4.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 402,028.2 ± 7,953.2 | — | 1,066,721.6 | — |
| `cModifyId` | `size=64` | 52,311.1 ± 275.8 | — | 136,262.0 | — |
| `cModifyId` | `size=8` | 8,330.6 ± 48.0 | — | 20,688.1 | — |
| `cReadId` | `size=512` | 204,774.9 ± 635.5 | — | 797,930.2 | — |
| `cReadId` | `size=64` | 25,937.7 ± 166.2 | — | 101,292.6 | — |
| `cReadId` | `size=8` | 3,948.8 ± 28.1 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 211,612.4 ± 2,589.3 | — | 797,932.2 | — |
| `cReadStreet` | `size=64` | 26,219.9 ± 129.8 | — | 101,293.1 | — |
| `cReadStreet` | `size=8` | 3,938.7 ± 60.7 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 403,464.1 ± 4,730.5 | — | 1,066,664.9 | — |
| `cReplaceId` | `size=64` | 51,905.3 ± 311.0 | — | 136,221.5 | — |
| `cReplaceId` | `size=8` | 8,293.5 ± 45.4 | — | 20,648.1 | — |
| `cSumPrices` | `size=512` | 346,480.3 ± 3,040.3 | — | 1,240,739.2 | — |
| `cSumPrices` | `size=64` | 43,270.3 ± 562.4 | — | 157,053.7 | — |
| `cSumPrices` | `size=8` | 6,216.0 ± 36.7 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 173.2 ± 0.9 | — | 0.0 | — |
| `jMiss` | `size=64` | 170.2 ± 7.7 | — | 0.0 | — |
| `jMiss` | `size=8` | 169.5 ± 7.0 | — | 0.0 | — |
| `jModifyId` | `size=512` | 5,620.7 ± 49.0 | — | 41,921.6 | — |
| `jModifyId` | `size=64` | 656.3 ± 4.2 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 145.1 ± 1.7 | — | 992.0 | — |
| `jReadId` | `size=512` | 34.6 ± 1.0 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.1 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=8` | 36.3 ± 2.1 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 180.0 ± 1.1 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 180.5 ± 1.1 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 179.5 ± 1.3 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 5,674.2 ± 40.2 | — | 41,897.6 | — |
| `jReplaceId` | `size=64` | 671.2 ± 6.7 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 145.9 ± 0.9 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 83,876.0 ± 1,194.6 | — | 63,664.0 | — |
| `jSumPrices` | `size=64` | 10,586.0 ± 110.2 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,492.2 ± 28.3 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 78.6 ± 3.1 | — | 312.0 | — |
| `MapDrillModify` | `-` | 40.3 ± 0.3 | — | 216.0 | — |
| `MapGet` | `-` | 2.6 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 56.8 ± 1.8 | — | 200.0 | — |
| `handEnvUse` | `-` | 74.4 ± 1.0 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 37.8 ± 1.7 | — | 216.0 | — |
| `handMapGet` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 53.0 ± 0.7 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.2 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.1 ± 0.0 | 5.6 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 35.0 ± 1.0 | 30.3 ± 0.5 | 152.0 | 176.0 |
| `Replace` | `-` | 5.0 ± 0.7 | 4.8 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 16,271.2 ± 1,201.9 | — | 43,036.2 | — |
| `Fold_powerEach` | `size=256` | 3,230.3 ± 14.3 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 373.8 ± 1.4 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 76.4 ± 0.3 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 57,429.2 ± 202.4 | — | 331,114.8 | — |
| `Modify_multiFocus` | `size=256` | 13,369.6 ± 77.8 | — | 76,134.2 | — |
| `Modify_multiFocus` | `size=32` | 1,598.1 ± 21.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 244.0 ± 4.5 | — | 1,341.3 | — |
| `Modify_powerEach` | `size=1024` | 34,555.7 ± 741.3 | — | 115,177.0 | — |
| `Modify_powerEach` | `size=256` | 8,084.0 ± 81.2 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,009.5 ± 7.3 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 190.4 ± 3.1 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 10,283.7 ± 538.4 | — | 65,578.7 | — |
| `naive_listMap` | `size=256` | 2,469.5 ± 10.5 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 306.6 ± 1.4 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 43.6 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,716.2 ± 138.6 | — | 16,129.2 | — |
| `naive_sumQty` | `size=256` | 684.4 ± 5.2 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 67.0 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 6.8 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 56.1 ± 1.4 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 199.4 ± 4.7 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 19.6 ± 0.1 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 33.6 ± 0.3 | — | 224.0 | — |
| `naive_constSum` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 31.2 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 12.8 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 23.2 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 207.4 ± 1.8 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 46.0 ± 0.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,087.7 ± 5.0 | — | 2,840.0 | — |
| `reuseUse` | `-` | 1,017.3 ± 13.6 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 22.3 ± 2.7 | 22.1 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.3 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 53.9 ± 0.2 | 68.3 ± 0.2 | 160.0 | 304.0 |
| `Modify_6` | `-` | 126.3 ± 2.7 | 112.4 ± 2.8 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 18.0 ± 0.2 | 18.6 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.4 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 6.4 ± 0.1 | 6.1 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 11.4 ± 0.2 | 12.0 ± 2.3 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 33,947.1 ± 1,445.5 | — | 105,610.7 | — |
| `ModifyNames` | `size=64` | 4,034.5 ± 19.2 | — | 12,584.1 | — |
| `ModifyNames` | `size=8` | 572.9 ± 13.7 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 121.2 ± 0.7 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 122.2 ± 1.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 121.7 ± 0.8 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.0 ± 0.7 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.1 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 34.6 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 100,976.0 ± 847.4 | — | 382,772.0 | — |
| `monocleModifyNames` | `size=64` | 10,240.4 ± 433.2 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,470.5 ± 22.7 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 53,788.6 ± 431.0 | — | 169,082.5 | — |
| `monocleModifyStreet` | `size=64` | 6,973.7 ± 27.7 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 931.6 ± 12.0 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 32,089.8 ± 179.8 | — | 69,789.5 | — |
| `monocleReadStreet` | `size=64` | 4,071.4 ± 16.6 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 479.1 ± 3.8 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 65,820.0 ± 330.4 | — | 226,299.5 | — |
| `naiveModifyNames` | `size=64` | 8,638.0 ± 43.8 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,124.7 ± 13.1 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 53,571.1 ± 279.7 | — | 169,060.9 | — |
| `naiveModifyStreet` | `size=64` | 6,805.6 ± 216.1 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 932.7 ± 6.3 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 32,354.8 ± 287.9 | — | 69,789.4 | — |
| `naiveReadStreet` | `size=64` | 4,063.0 ± 38.5 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 488.9 ± 8.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 236,200.2 ± 4,432.3 | — | 609,925.3 | — |
| `Names` | `size=64` | 29,779.3 ± 284.8 | — | 78,779.1 | — |
| `Names` | `size=8` | 4,198.0 ± 72.5 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 254,559.8 ± 5,199.2 | — | 679,444.0 | — |
| `NamesIor` | `size=64` | 32,783.4 ± 652.2 | — | 87,533.0 | — |
| `NamesIor` | `size=8` | 4,284.9 ± 31.0 | — | 11,592.1 | — |
| `Street` | `size=512` | 1,029.8 ± 8.4 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,030.5 ± 22.0 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,045.5 ± 52.5 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,032.1 ± 12.8 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,061.5 ± 36.9 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,032.6 ± 22.3 | — | 2,736.0 | — |
| `directNames` | `size=512` | 231,988.2 ± 3,296.7 | — | 613,985.9 | — |
| `directNames` | `size=64` | 29,337.7 ± 621.0 | — | 76,679.0 | — |
| `directNames` | `size=8` | 4,033.9 ± 146.3 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,023.0 ± 5.7 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,022.3 ± 9.7 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,027.5 ± 18.9 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 230,402.2 ± 4,630.5 | — | 605,776.7 | — |
| `hcursorNames` | `size=64` | 29,306.9 ± 703.9 | — | 77,268.7 | — |
| `hcursorNames` | `size=8` | 4,039.6 ± 34.1 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,103.0 ± 7.8 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,097.6 ± 10.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,110.6 ± 27.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 230,377.9 ± 2,272.3 | — | 1,121,762.8 | — |
| `monocleNames` | `size=64` | 26,062.1 ± 285.1 | — | 132,765.7 | — |
| `monocleNames` | `size=8` | 3,964.8 ± 71.7 | — | 19,488.1 | — |
| `monocleStreet` | `size=512` | 180,881.8 ± 11,053.9 | — | 908,025.5 | — |
| `monocleStreet` | `size=64` | 22,191.4 ± 177.4 | — | 113,799.4 | — |
| `monocleStreet` | `size=8` | 3,330.4 ± 16.6 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 193,295.1 ± 5,152.6 | — | 965,265.9 | — |
| `naiveNames` | `size=64` | 23,844.4 ± 257.4 | — | 120,845.2 | — |
| `naiveNames` | `size=8` | 3,521.4 ± 22.8 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 176,141.3 ± 1,437.9 | — | 902,558.4 | — |
| `naiveStreet` | `size=64` | 22,172.0 ± 303.5 | — | 113,778.0 | — |
| `naiveStreet` | `size=8` | 3,334.1 ± 35.1 | — | 17,050.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 5,830.2 ± 92.1 | — | 42,029.2 | — |
| `ModifyStreet` | `size=64` | 846.3 ± 7.4 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 349.6 ± 19.6 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 184.4 ± 2.5 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 182.2 ± 1.7 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 182.1 ± 3.2 | — | 128.0 | — |
| `SumPrices` | `size=512` | 84,604.5 ± 775.9 | — | 63,715.8 | — |
| `SumPrices` | `size=64` | 10,519.7 ± 58.3 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,509.8 ± 16.5 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 161,337.5 ± 2,442.9 | — | 333,576.1 | — |
| `monocleModifyStreet` | `size=64` | 19,108.1 ± 197.2 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,335.8 ± 17.6 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 87,933.8 ± 1,580.2 | — | 193,227.3 | — |
| `monocleReadStreet` | `size=64` | 11,125.2 ± 211.4 | — | 24,705.1 | — |
| `monocleReadStreet` | `size=8` | 1,829.6 ± 10.2 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 403,160.9 ± 2,928.3 | — | 1,190,729.1 | — |
| `monocleSumPrices` | `size=64` | 15,533.2 ± 427.0 | — | 46,876.3 | — |
| `monocleSumPrices` | `size=8` | 2,673.6 ± 14.8 | — | 6,640.1 | — |
| `naiveModifyStreet` | `size=512` | 161,649.4 ± 7,222.5 | — | 333,567.1 | — |
| `naiveModifyStreet` | `size=64` | 19,040.9 ± 225.5 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,313.4 ± 10.8 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 85,327.3 ± 1,929.4 | — | 193,225.0 | — |
| `naiveReadStreet` | `size=64` | 11,016.0 ± 310.4 | — | 24,705.1 | — |
| `naiveReadStreet` | `size=8` | 1,839.8 ± 8.9 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 92,440.2 ± 1,902.1 | — | 230,119.1 | — |
| `naiveSumPrices` | `size=64` | 11,884.2 ± 197.7 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,937.0 ± 7.1 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,115.7 ± 348.0 | — | 451.5 | — |
| `nativeReadStreet` | `size=64` | 4,275.9 ± 95.6 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 794.2 ± 9.2 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 59,431.7 ± 996.1 | — | 86,271.5 | — |
| `nativeSumPrices` | `size=64` | 7,414.6 ± 118.0 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,175.3 ± 38.1 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 140,316.8 ± 831.6 | — | 624,398.1 | — |
| `TransformDeep` | `n=512` | 16,687.7 ± 100.7 | — | 57,361.7 | — |
| `TransformDeep` | `n=64` | 1,932.1 ± 5.5 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 135,687.9 ± 1,024.1 | 173,520.1 ± 949.7 | 655,362.8 | 753,742.3 |
| `TransformExpr` | `n=512` | 16,921.0 ± 47.6 | 16,628.8 ± 100.2 | 81,825.7 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,083.7 ± 6.8 | 2,677.2 ± 19.1 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 147,610.8 ± 923.4 | — | 786,611.4 | — |
| `UniverseDeep` | `n=512` | 17,928.4 ± 75.5 | — | 98,377.8 | — |
| `UniverseDeep` | `n=64` | 2,121.0 ± 15.7 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 147,441.7 ± 691.0 | 3,990,196.4 ± 171,821.4 | 786,419.3 | 4,688,578.0 |
| `UniverseExpr` | `n=512` | 17,379.7 ± 127.6 | 295,830.3 ± 1,740.8 | 98,185.8 | 475,022.4 |
| `UniverseExpr` | `n=64` | 2,076.1 ± 11.9 | 27,464.8 ± 1,131.9 | 12,168.0 | 45,424.5 |
| `UniverseJson` | `n=4096` | 254,478.0 ± 2,896.5 | 4,374,190.3 ± 205,346.3 | 786,497.2 | 6,490,719.6 |
| `UniverseJson` | `n=512` | 28,331.9 ± 702.3 | 329,382.4 ± 1,835.8 | 98,186.9 | 699,929.6 |
| `UniverseJson` | `n=64` | 3,234.1 ± 14.9 | 30,921.9 ± 173.1 | 12,168.1 | 73,208.6 |
| `visitorTransformDeep` | `n=4096` | 50,295.1 ± 213.8 | — | 163,892.6 | — |
| `visitorTransformDeep` | `n=512` | 5,767.0 ± 12.7 | — | 20,496.6 | — |
| `visitorTransformDeep` | `n=64` | 602.7 ± 10.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 70,401.6 ± 207.9 | — | 360,475.2 | — |
| `visitorTransformExpr` | `n=512` | 8,522.2 ± 30.4 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,077.8 ± 2.5 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 72,613.4 ± 247.7 | — | 196,716.9 | — |
| `visitorUniverseDeep` | `n=512` | 8,894.0 ± 31.1 | — | 24,632.9 | — |
| `visitorUniverseDeep` | `n=64` | 945.2 ± 3.6 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 54,045.6 ± 345.2 | — | 196,655.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,636.8 ± 26.6 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 786.9 ± 2.5 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 194,733.7 ± 12,526.0 | — | 469,789.7 | — |
| `visitorUniverseJson` | `n=512` | 20,689.1 ± 155.8 | — | 40,954.1 | — |
| `visitorUniverseJson` | `n=64` | 1,998.2 ± 11.5 | — | 5,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,584.8 ± 70.7 | — | 41,414.4 | — |
| `Modify_powerEach` | `size=16` | 259.0 ± 1.2 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,321.8 ± 49.1 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 117.7 ± 1.7 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 56,063.2 ± 628.5 | — | 164,379.3 | — |
| `Modify_powerEach` | `size=64` | 868.3 ± 7.7 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 61,195.0 ± 468.5 | — | 279,433.6 | — |
| `monocle_powerEach` | `size=16` | 720.2 ± 18.4 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 21,811.2 ± 168.3 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 248.8 ± 2.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 214,508.7 ± 4,920.7 | — | 967,901.3 | — |
| `monocle_powerEach` | `size=64` | 2,556.8 ± 14.8 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 5,041.2 ± 37.6 | — | 28,730.4 | — |
| `naive_powerEach` | `size=16` | 91.3 ± 0.8 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,309.2 ± 10.8 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 28.2 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 20,488.9 ± 230.0 | — | 114,777.4 | — |
| `naive_powerEach` | `size=64` | 341.5 ± 5.4 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 77,988.5 ± 726.8 | — | 210,563.3 | — |
| `Modify_nested` | `size=16` | 1,557.3 ± 18.4 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 19,125.6 ± 309.1 | — | 53,676.1 | — |
| `Modify_nested` | `size=4` | 717.7 ± 12.9 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 5,025.3 ± 106.3 | — | 14,616.6 | — |
| `monocle_nested` | `size=1024` | 251,617.2 ± 1,070.0 | — | 1,118,858.1 | — |
| `monocle_nested` | `size=16` | 3,110.8 ± 90.9 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 96,756.5 ± 1,251.5 | — | 430,210.8 | — |
| `monocle_nested` | `size=4` | 1,428.5 ± 82.4 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 10,315.8 ± 61.4 | — | 58,902.5 | — |
| `naive_nested` | `size=1024` | 21,567.3 ± 126.1 | — | 115,071.3 | — |
| `naive_nested` | `size=16` | 376.2 ± 3.7 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,545.0 ± 58.3 | — | 29,019.5 | — |
| `naive_nested` | `size=4` | 137.2 ± 1.6 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,331.6 ± 13.4 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,482.6 ± 152.2 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 26,203.6 ± 999.7 | — | 104,685.3 | — |
| `Modify_sparse` | `size=32` | 377.1 ± 11.8 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 6,308.0 ± 139.5 | — | 24,785.7 | — |
| `Modify_sparse` | `size=8` | 124.9 ± 0.9 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,765.6 ± 35.6 | — | 27,784.2 | — |
| `monocle_sparse` | `size=2048` | 114,148.4 ± 1,084.1 | — | 523,150.9 | — |
| `monocle_sparse` | `size=32` | 1,249.7 ± 8.7 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 33,984.9 ± 293.1 | — | 166,685.7 | — |
| `monocle_sparse` | `size=8` | 372.3 ± 5.0 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 343.6 ± 3.5 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,894.8 ± 72.7 | — | 24,612.9 | — |
| `naive_sparse` | `size=32` | 88.3 ± 1.0 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,407.1 ± 10.0 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 24.8 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.2 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.6 ± 0.0 | 2.8 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.6 ± 0.0 | 2.9 ± 0.3 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.2 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 18.2 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 35.2 ± 5.5 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 10.7 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 18.6 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 102,934.7 ± 771.9 | — | 589,712.7 | — |
| `Cata` | `-` | 71,760.4 ± 531.9 | — | 197,568.5 | — |
| `Hylo` | `-` | 84,885.7 ± 723.2 | — | 295,848.6 | — |
| `drosteAna` | `-` | 52,136.4 ± 430.4 | — | 327,632.4 | — |
| `drosteCata` | `-` | 40,585.6 ± 347.8 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 58,662.2 ± 7,580.5 | — | 328,640.4 | — |
| `handAna` | `-` | 27,919.0 ± 529.3 | — | 163,816.2 | — |
| `handCata` | `-` | 13,641.8 ± 10.8 | — | 0.1 | — |
| `handHylo` | `-` | 10,983.9 ± 47.1 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.9 ± 0.1 | 29.9 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 25.7 ± 0.1 | 59.4 ± 0.6 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 4.8 ± 0.1 | 4.7 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,490.8 ± 53.4 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 628.8 ± 9.9 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 56.3 ± 0.4 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,971.9 ± 25.5 | 31,694.8 ± 258.0 | 12,312.5 | 162,581.0 |
| `FoldPrices` | `size=64` | 319.6 ± 1.9 | 2,744.4 ± 43.7 | 1,560.0 | 15,424.1 |
| `FoldPrices` | `size=8` | 43.3 ± 0.5 | 407.1 ± 3.5 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 8,225.5 ± 120.1 | 34,307.2 ± 250.0 | 36,897.3 | 176,925.3 |
| `Modify` | `size=64` | 895.6 ± 4.5 | 2,282.9 ± 17.7 | 4,640.0 | 14,448.1 |
| `Modify` | `size=8` | 104.6 ± 0.5 | 350.3 ± 2.3 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 11.4 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 124.6 ± 1.3 | — | 528.0 | — |
| `ServiceGet` | `-` | 4.6 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 99.6 ± 0.6 | — | 456.0 | — |
| `handDrillGet` | `-` | 19.2 ± 0.2 | — | 120.0 | — |
| `handDrillModify` | `-` | 121.4 ± 0.7 | — | 648.0 | — |
| `handServiceGet` | `-` | 19.2 ± 0.2 | — | 120.0 | — |
| `handServiceReplace` | `-` | 106.5 ± 0.4 | — | 576.0 | — |


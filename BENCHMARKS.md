# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `188f10bd35c7a67fb18eaf4378398f98ec7c783e` · date: `2026-10-01` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.7 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 11.6 ± 0.0 | 7.9 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 22.3 ± 0.0 | 19.3 ± 0.7 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 118.5 ± 1.1 | — | 720.0 | — |
| `ModifyCountry` | `-` | 223.2 ± 4.9 | — | 928.0 | — |
| `ModifyPartner` | `-` | 270.5 ± 3.1 | — | 984.0 | — |
| `ReadCountry` | `-` | 140.6 ± 1.9 | — | 520.0 | — |
| `ReadPartner` | `-` | 159.3 ± 0.8 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 232.4 ± 0.7 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,072.3 ± 15.9 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,339.3 ± 16.9 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 3,761.2 ± 69.4 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,264.5 ± 27.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,590.0 ± 7.5 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 681.3 ± 3.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 441.0 ± 1.0 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 309.3 ± 0.4 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 331.8 ± 0.6 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,050.3 ± 4.0 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,040.4 ± 26.3 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 43.3 ± 0.1 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 492.6 ± 3.8 | — | 568.0 | — |
| `handwritten_stream` | `-` | 116.0 ± 0.5 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 563.7 ± 19.6 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,303.4 ± 12.4 | — | 4,674.7 | — |
| `ClickToJson` | `-` | 2,126.8 ± 20.4 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 562.0 ± 25.7 | — | 2,050.7 | — |
| `WideToJson` | `-` | 458.3 ± 11.2 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,085.1 ± 6.3 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,298.0 ± 40.6 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 701.5 ± 8.3 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,412.4 ± 6.8 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 165.4 ± 0.5 | — | 880.0 | — |
| `decode_native` | `-` | 13.2 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 160.4 ± 0.2 | — | 880.0 | — |
| `encode_bridged` | `-` | 174.5 ± 1.1 | — | 1,224.0 | — |
| `encode_native` | `-` | 9.3 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 173.3 ± 1.1 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 78.7 ± 0.4 | — | 432.0 | — |
| `fieldGet_native` | `-` | 78.9 ± 1.8 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 287.0 ± 1.5 | — | 1,472.0 | — |
| `rootGet_native` | `-` | 129.5 ± 1.0 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 16.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 16.6 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 16.3 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 16.2 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 3.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 24.1 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 29.4 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 3.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 3.1 ± 0.0 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 476.8 ± 18.5 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 453.0 ± 6.5 | — | 744.0 | — |
| `encode_hand` | `-` | 111.2 ± 0.2 | — | 768.0 | — |
| `encode_positional` | `-` | 5,171.4 ± 61.5 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 6,135.7 ± 27.7 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 3.9 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 17.0 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 31.6 ± 0.1 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 16.8 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 13.4 ± 0.9 | — | 40.0 | — |
| `reuseLens3` | `-` | 37.3 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 104.2 ± 0.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 47.4 ± 0.4 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 3,386.0 ± 4.4 | 3,371.4 ± 3.9 | 14,080.6 | 14,080.5 |
| `FoldMap` | `size=64` | 299.6 ± 1.4 | 294.2 ± 0.6 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 16.3 ± 0.0 | 17.1 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,428.9 ± 15.0 | 2,422.6 ± 18.6 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 287.4 ± 0.3 | 287.3 ± 0.3 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 37.2 ± 0.0 | 37.2 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 1.6 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.5 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 1.7 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 13.9 ± 0.0 | 7.2 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 24.7 ± 0.2 | 20.5 ± 1.0 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 2.8 ± 0.0 | 2.9 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 2.5 ± 0.0 | 2.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 319,437.0 ± 2,025.9 | — | 1,066,682.8 | — |
| `cModifyId` | `size=64` | 41,346.6 ± 348.8 | — | 136,258.9 | — |
| `cModifyId` | `size=8` | 6,756.0 ± 94.5 | — | 20,704.1 | — |
| `cReadId` | `size=512` | 159,126.6 ± 460.8 | — | 797,917.3 | — |
| `cReadId` | `size=64` | 20,031.4 ± 88.3 | — | 101,288.6 | — |
| `cReadId` | `size=8` | 3,079.0 ± 5.4 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 159,360.9 ± 366.8 | — | 797,917.3 | — |
| `cReadStreet` | `size=64` | 20,860.4 ± 1,265.7 | — | 101,288.7 | — |
| `cReadStreet` | `size=8` | 3,108.6 ± 17.4 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 321,046.6 ± 1,427.6 | — | 1,066,643.3 | — |
| `cReplaceId` | `size=64` | 41,578.7 ± 287.2 | — | 136,218.8 | — |
| `cReplaceId` | `size=8` | 6,784.4 ± 95.9 | — | 20,640.1 | — |
| `cSumPrices` | `size=512` | 264,487.1 ± 1,078.1 | — | 1,240,715.2 | — |
| `cSumPrices` | `size=64` | 32,948.9 ± 126.3 | — | 157,385.1 | — |
| `cSumPrices` | `size=8` | 4,645.6 ± 36.8 | — | 22,682.7 | — |
| `jMiss` | `size=512` | 150.3 ± 0.9 | — | 0.0 | — |
| `jMiss` | `size=64` | 144.8 ± 3.5 | — | 0.0 | — |
| `jMiss` | `size=8` | 147.5 ± 3.7 | — | 0.0 | — |
| `jModifyId` | `size=512` | 2,658.9 ± 6.0 | — | 41,920.8 | — |
| `jModifyId` | `size=64` | 249.5 ± 11.6 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 80.9 ± 2.6 | — | 984.0 | — |
| `jReadId` | `size=512` | 28.7 ± 1.8 | — | 48.0 | — |
| `jReadId` | `size=64` | 28.6 ± 2.0 | — | 48.0 | — |
| `jReadId` | `size=8` | 28.5 ± 2.0 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 160.4 ± 0.6 | — | 128.0 | — |
| `jReadStreet` | `size=64` | 159.9 ± 5.0 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 163.0 ± 2.8 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 2,658.7 ± 8.0 | — | 41,896.8 | — |
| `jReplaceId` | `size=64` | 241.9 ± 7.1 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 78.6 ± 0.4 | — | 936.0 | — |
| `jSumPrices` | `size=512` | 68,238.0 ± 140.7 | — | 63,670.5 | — |
| `jSumPrices` | `size=64` | 8,306.8 ± 21.5 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,111.8 ± 6.5 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 51.8 ± 0.7 | — | 312.0 | — |
| `MapDrillModify` | `-` | 32.7 ± 1.4 | — | 216.0 | — |
| `MapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 40.3 ± 0.2 | — | 200.0 | — |
| `handEnvUse` | `-` | 50.1 ± 0.1 | — | 304.0 | — |
| `handMapDrillModify` | `-` | 27.0 ± 0.6 | — | 216.0 | — |
| `handMapGet` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 38.2 ± 2.3 | — | 184.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 3.1 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 29.8 ± 0.6 | 23.2 ± 0.0 | 152.0 | 176.0 |
| `Replace` | `-` | 2.7 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 12,832.4 ± 336.6 | — | 43,035.3 | — |
| `Fold_powerEach` | `size=256` | 2,872.1 ± 51.5 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 299.4 ± 1.6 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 60.8 ± 0.3 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 43,998.2 ± 212.9 | — | 331,141.3 | — |
| `Modify_multiFocus` | `size=256` | 10,230.9 ± 46.9 | — | 76,112.7 | — |
| `Modify_multiFocus` | `size=32` | 1,066.3 ± 1.7 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 159.8 ± 1.1 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 30,251.0 ± 423.1 | — | 115,175.9 | — |
| `Modify_powerEach` | `size=256` | 7,049.1 ± 35.3 | — | 26,080.5 | — |
| `Modify_powerEach` | `size=32` | 793.4 ± 0.9 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 143.7 ± 0.3 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 7,723.9 ± 42.8 | — | 65,578.0 | — |
| `naive_listMap` | `size=256` | 1,982.8 ± 8.8 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 212.9 ± 0.2 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 30.5 ± 0.1 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 3,739.4 ± 76.9 | — | 16,129.0 | — |
| `naive_sumQty` | `size=256` | 680.7 ± 45.1 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 49.9 ± 0.1 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 5.5 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 52.9 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 146.8 ± 0.2 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 13.0 ± 0.2 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 21.4 ± 0.0 | — | 224.0 | — |
| `naive_constSum` | `-` | 1.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 25.7 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 6.2 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 11.4 ± 0.0 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 127.4 ± 0.2 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 36.7 ± 0.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 793.3 ± 3.5 | — | 2,816.0 | — |
| `reuseUse` | `-` | 759.9 ± 24.5 | — | 2,672.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 19.7 ± 3.0 | 17.6 ± 0.0 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 47.4 ± 0.2 | 52.6 ± 0.1 | 160.0 | 304.0 |
| `Modify_6` | `-` | 114.0 ± 0.2 | 86.8 ± 0.3 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 13.4 ± 0.0 | 13.0 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 3.5 ± 0.0 | 2.9 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 6.0 ± 0.0 | 5.9 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 26,891.3 ± 206.4 | — | 97,406.3 | — |
| `ModifyNames` | `size=64` | 3,291.0 ± 9.0 | — | 12,584.1 | — |
| `ModifyNames` | `size=8` | 442.6 ± 3.0 | — | 2,048.0 | — |
| `ModifyStreet` | `size=512` | 93.3 ± 0.6 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 93.2 ± 0.6 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 98.2 ± 8.3 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 26.9 ± 0.1 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 27.2 ± 0.7 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 27.4 ± 0.8 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 79,596.4 ± 178.0 | — | 382,782.6 | — |
| `monocleModifyNames` | `size=64` | 7,998.8 ± 396.7 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,059.6 ± 42.8 | — | 5,416.0 | — |
| `monocleModifyStreet` | `size=512` | 43,496.6 ± 303.0 | — | 169,079.0 | — |
| `monocleModifyStreet` | `size=64` | 5,374.0 ± 424.1 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 695.3 ± 5.1 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 26,958.2 ± 242.9 | — | 69,784.7 | — |
| `monocleReadStreet` | `size=64` | 3,443.0 ± 17.3 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 386.9 ± 1.3 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 54,911.2 ± 174.8 | — | 226,293.6 | — |
| `naiveModifyNames` | `size=64` | 6,696.8 ± 407.9 | — | 27,928.2 | — |
| `naiveModifyNames` | `size=8` | 829.9 ± 4.6 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 43,090.3 ± 973.8 | — | 169,055.9 | — |
| `naiveModifyStreet` | `size=64` | 5,387.6 ± 410.6 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 691.8 ± 2.9 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 26,924.9 ± 201.1 | — | 69,784.5 | — |
| `naiveReadStreet` | `size=64` | 3,446.1 ± 17.2 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 388.4 ± 4.3 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 172,696.3 ± 1,454.3 | — | 605,770.4 | — |
| `Names` | `size=64` | 22,092.3 ± 770.6 | — | 78,778.3 | — |
| `Names` | `size=8` | 2,966.3 ± 29.3 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 180,203.0 ± 2,165.1 | — | 683,488.4 | — |
| `NamesIor` | `size=64` | 23,250.5 ± 255.8 | — | 87,530.4 | — |
| `NamesIor` | `size=8` | 3,066.1 ± 35.9 | — | 11,592.1 | — |
| `Street` | `size=512` | 758.9 ± 8.4 | — | 2,720.6 | — |
| `Street` | `size=64` | 751.6 ± 10.0 | — | 2,720.1 | — |
| `Street` | `size=8` | 754.5 ± 6.7 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 755.3 ± 6.0 | — | 2,736.6 | — |
| `StreetIor` | `size=64` | 757.1 ± 7.9 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 752.3 ± 6.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 173,434.9 ± 2,166.7 | — | 613,939.0 | — |
| `directNames` | `size=64` | 21,739.8 ± 772.9 | — | 77,194.3 | — |
| `directNames` | `size=8` | 2,860.8 ± 17.5 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 744.6 ± 8.5 | — | 2,744.6 | — |
| `directStreet` | `size=64` | 770.6 ± 8.2 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 756.7 ± 4.4 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 172,674.7 ± 595.2 | — | 609,834.4 | — |
| `hcursorNames` | `size=64` | 21,053.2 ± 206.2 | — | 77,778.2 | — |
| `hcursorNames` | `size=8` | 2,928.4 ± 49.1 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 786.8 ± 6.1 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 798.0 ± 26.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 793.2 ± 7.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 176,456.2 ± 1,096.0 | — | 1,121,726.6 | — |
| `monocleNames` | `size=64` | 19,087.6 ± 32.0 | — | 132,751.0 | — |
| `monocleNames` | `size=8` | 2,844.6 ± 15.1 | — | 19,514.7 | — |
| `monocleStreet` | `size=512` | 132,970.7 ± 885.3 | — | 908,004.0 | — |
| `monocleStreet` | `size=64` | 16,323.8 ± 32.5 | — | 113,820.1 | — |
| `monocleStreet` | `size=8` | 2,412.5 ± 6.0 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 144,378.5 ± 1,138.2 | — | 965,230.4 | — |
| `naiveNames` | `size=64` | 17,638.5 ± 49.4 | — | 120,852.2 | — |
| `naiveNames` | `size=8` | 2,549.6 ± 5.7 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 133,744.7 ± 794.3 | — | 908,020.5 | — |
| `naiveStreet` | `size=64` | 16,364.5 ± 40.5 | — | 113,769.5 | — |
| `naiveStreet` | `size=8` | 2,412.9 ± 11.8 | — | 17,040.0 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 2,837.7 ± 5.2 | — | 42,026.5 | — |
| `ModifyStreet` | `size=64` | 382.0 ± 3.0 | — | 5,432.0 | — |
| `ModifyStreet` | `size=8` | 229.6 ± 0.5 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 161.9 ± 2.0 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 162.2 ± 0.8 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 161.2 ± 2.9 | — | 72.0 | — |
| `SumPrices` | `size=512` | 67,638.4 ± 959.1 | — | 63,712.5 | — |
| `SumPrices` | `size=64` | 8,243.2 ± 45.6 | — | 8,121.0 | — |
| `SumPrices` | `size=8` | 1,114.0 ± 4.2 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 127,045.7 ± 667.3 | — | 333,562.7 | — |
| `monocleModifyStreet` | `size=64` | 15,368.0 ± 51.2 | — | 30,081.5 | — |
| `monocleModifyStreet` | `size=8` | 2,517.2 ± 13.0 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 72,222.4 ± 657.5 | — | 193,213.8 | — |
| `monocleReadStreet` | `size=64` | 9,175.5 ± 45.5 | — | 24,704.9 | — |
| `monocleReadStreet` | `size=8` | 1,399.4 ± 13.3 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 346,613.1 ± 1,052.5 | — | 1,190,680.7 | — |
| `monocleSumPrices` | `size=64` | 12,607.5 ± 33.8 | — | 47,388.0 | — |
| `monocleSumPrices` | `size=8` | 1,923.6 ± 12.3 | — | 6,640.0 | — |
| `naiveModifyStreet` | `size=512` | 127,107.7 ± 857.5 | — | 333,537.7 | — |
| `naiveModifyStreet` | `size=64` | 15,458.5 ± 141.2 | — | 30,057.6 | — |
| `naiveModifyStreet` | `size=8` | 2,509.9 ± 7.4 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 72,131.9 ± 476.2 | — | 193,213.8 | — |
| `naiveReadStreet` | `size=64` | 9,137.7 ± 98.6 | — | 24,704.9 | — |
| `naiveReadStreet` | `size=8` | 1,395.6 ± 5.7 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 76,418.6 ± 826.9 | — | 230,105.4 | — |
| `naiveSumPrices` | `size=64` | 9,617.5 ± 41.9 | — | 29,337.0 | — |
| `naiveSumPrices` | `size=8` | 1,464.3 ± 12.4 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 27,504.2 ± 1,383.0 | — | 446.8 | — |
| `nativeReadStreet` | `size=64` | 3,794.2 ± 131.6 | — | 424.4 | — |
| `nativeReadStreet` | `size=8` | 674.8 ± 4.8 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 50,756.3 ± 203.7 | — | 86,268.3 | — |
| `nativeSumPrices` | `size=64` | 6,238.7 ± 15.8 | — | 10,920.7 | — |
| `nativeSumPrices` | `size=8` | 912.1 ± 2.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 98,230.9 ± 652.3 | — | 624,367.5 | — |
| `TransformDeep` | `n=512` | 10,238.3 ± 44.7 | — | 57,361.0 | — |
| `TransformDeep` | `n=64` | 1,133.9 ± 6.0 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 102,584.8 ± 350.7 | 134,250.0 ± 127.2 | 655,338.7 | 753,713.7 |
| `TransformExpr` | `n=512` | 12,655.2 ± 30.2 | 12,588.8 ± 30.3 | 81,825.3 | 69,585.3 |
| `TransformExpr` | `n=64` | 1,560.3 ± 2.7 | 2,058.4 ± 2.8 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 101,411.2 ± 2,959.8 | — | 786,577.8 | — |
| `UniverseDeep` | `n=512` | 12,640.9 ± 38.9 | — | 98,377.3 | — |
| `UniverseDeep` | `n=64` | 1,510.4 ± 2.1 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 96,801.2 ± 758.2 | 2,261,662.3 ± 201,336.3 | 786,382.5 | 4,687,324.7 |
| `UniverseExpr` | `n=512` | 12,228.9 ± 22.7 | 127,478.5 ± 7,966.9 | 98,185.3 | 475,005.1 |
| `UniverseExpr` | `n=64` | 1,471.2 ± 3.5 | 11,213.5 ± 35.7 | 12,168.0 | 45,424.2 |
| `UniverseJson` | `n=4096` | 186,596.9 ± 3,480.1 | 2,485,508.7 ± 197,941.2 | 786,447.8 | 6,489,350.6 |
| `UniverseJson` | `n=512` | 20,968.5 ± 70.1 | 151,652.7 ± 871.6 | 98,186.1 | 699,911.5 |
| `UniverseJson` | `n=64` | 2,455.0 ± 4.8 | 14,457.2 ± 95.8 | 12,168.0 | 73,208.3 |
| `visitorTransformDeep` | `n=4096` | 35,804.6 ± 515.6 | — | 163,882.1 | — |
| `visitorTransformDeep` | `n=512` | 3,499.7 ± 55.0 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 345.5 ± 0.6 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 52,289.4 ± 241.1 | — | 360,462.1 | — |
| `visitorTransformExpr` | `n=512` | 6,467.2 ± 21.8 | — | 45,032.7 | — |
| `visitorTransformExpr` | `n=64` | 789.0 ± 1.4 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 46,897.3 ± 935.6 | — | 196,698.1 | — |
| `visitorUniverseDeep` | `n=512` | 5,627.3 ± 22.7 | — | 24,632.6 | — |
| `visitorUniverseDeep` | `n=64` | 616.9 ± 5.8 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 44,867.3 ± 889.4 | — | 196,648.7 | — |
| `visitorUniverseExpr` | `n=512` | 5,466.1 ± 10.2 | — | 24,584.6 | — |
| `visitorUniverseExpr` | `n=64` | 628.9 ± 1.1 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 148,780.6 ± 2,979.0 | — | 524,372.3 | — |
| `visitorUniverseJson` | `n=512` | 16,372.6 ± 58.3 | — | 40,953.7 | — |
| `visitorUniverseJson` | `n=64` | 1,577.4 ± 36.5 | — | 4,424.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 10,585.8 ± 74.1 | — | 41,413.0 | — |
| `Modify_powerEach` | `size=16` | 208.6 ± 1.3 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,657.1 ± 44.4 | — | 10,688.4 | — |
| `Modify_powerEach` | `size=4` | 90.2 ± 0.4 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 42,049.1 ± 1,185.6 | — | 164,356.5 | — |
| `Modify_powerEach` | `size=64` | 629.4 ± 7.8 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 46,192.5 ± 317.2 | — | 279,423.4 | — |
| `monocle_powerEach` | `size=16` | 460.2 ± 25.5 | — | 3,736.0 | — |
| `monocle_powerEach` | `size=256` | 16,615.6 ± 99.7 | — | 107,338.6 | — |
| `monocle_powerEach` | `size=4` | 181.5 ± 0.4 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 141,577.0 ± 3,519.3 | — | 967,782.5 | — |
| `monocle_powerEach` | `size=64` | 1,707.1 ± 22.6 | — | 14,520.1 | — |
| `naive_powerEach` | `size=1024` | 4,418.4 ± 16.2 | — | 28,730.1 | — |
| `naive_powerEach` | `size=16` | 83.5 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,328.7 ± 7.0 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 20.7 ± 0.0 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 17,545.4 ± 72.6 | — | 114,772.6 | — |
| `naive_powerEach` | `size=64` | 328.5 ± 0.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 58,627.1 ± 831.1 | — | 210,442.7 | — |
| `Modify_nested` | `size=16` | 1,092.0 ± 21.6 | — | 4,744.0 | — |
| `Modify_nested` | `size=256` | 15,108.8 ± 40.8 | — | 53,753.5 | — |
| `Modify_nested` | `size=4` | 495.2 ± 7.2 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 3,871.7 ± 17.4 | — | 14,669.8 | — |
| `monocle_nested` | `size=1024` | 189,566.4 ± 2,414.4 | — | 1,118,745.1 | — |
| `monocle_nested` | `size=16` | 2,090.0 ± 42.6 | — | 15,776.1 | — |
| `monocle_nested` | `size=256` | 69,855.8 ± 201.9 | — | 430,202.3 | — |
| `monocle_nested` | `size=4` | 1,010.0 ± 73.3 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 7,605.4 ± 207.9 | — | 58,907.5 | — |
| `naive_nested` | `size=1024` | 18,285.7 ± 225.7 | — | 115,065.3 | — |
| `naive_nested` | `size=16` | 298.5 ± 4.2 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,932.7 ± 9.9 | — | 29,019.1 | — |
| `naive_nested` | `size=4` | 109.3 ± 7.0 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,164.9 ± 10.5 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,075.4 ± 32.0 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 20,396.4 ± 707.8 | — | 104,688.1 | — |
| `Modify_sparse` | `size=32` | 277.0 ± 1.6 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 4,990.8 ± 19.8 | — | 24,785.4 | — |
| `Modify_sparse` | `size=8` | 93.2 ± 0.3 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,403.2 ± 24.3 | — | 27,784.2 | — |
| `monocle_sparse` | `size=2048` | 78,608.6 ± 2,674.0 | — | 523,132.7 | — |
| `monocle_sparse` | `size=32` | 809.1 ± 36.6 | — | 7,024.0 | — |
| `monocle_sparse` | `size=512` | 25,613.2 ± 81.1 | — | 166,679.4 | — |
| `monocle_sparse` | `size=8` | 223.0 ± 5.4 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 245.0 ± 0.5 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,089.5 ± 17.9 | — | 24,611.5 | — |
| `naive_sparse` | `size=32` | 62.8 ± 0.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,029.4 ± 3.5 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 18.9 ± 0.0 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.0 ± 0.0 | 2.1 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 1.9 ± 0.0 | 2.0 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 1.9 ± 0.0 | 2.0 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.0 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 15.9 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 27.7 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 1.9 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 5.3 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 8.8 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 77,932.1 ± 1,060.7 | — | 589,712.6 | — |
| `Cata` | `-` | 58,588.2 ± 469.1 | — | 197,568.4 | — |
| `Hylo` | `-` | 64,834.0 ± 1,236.7 | — | 295,848.5 | — |
| `drosteAna` | `-` | 35,731.5 ± 68.3 | — | 327,632.3 | — |
| `drosteCata` | `-` | 31,273.8 ± 115.6 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 38,706.4 ± 112.2 | — | 328,640.3 | — |
| `handAna` | `-` | 18,828.1 ± 19.9 | — | 163,816.1 | — |
| `handCata` | `-` | 10,469.7 ± 10.8 | — | 0.1 | — |
| `handHylo` | `-` | 7,911.5 ± 573.3 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 1.9 ± 0.0 | 1.9 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 9.5 ± 0.0 | 21.5 ± 0.2 | 72.0 | 168.0 |
| `Modify_6` | `-` | 18.5 ± 0.1 | 44.6 ± 0.0 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 2.6 ± 0.0 | 2.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,420.3 ± 34.2 | — | 20,200.7 | — |
| `FoldNested` | `size=64` | 417.7 ± 0.5 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 39.6 ± 0.1 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,450.1 ± 9.2 | 24,531.9 ± 612.2 | 12,312.4 | 162,579.9 |
| `FoldPrices` | `size=64` | 287.5 ± 0.3 | 1,736.0 ± 47.4 | 1,560.0 | 15,424.0 |
| `FoldPrices` | `size=8` | 37.2 ± 0.0 | 235.2 ± 0.4 | 216.0 | 2,016.0 |
| `Modify` | `size=512` | 6,529.8 ± 165.3 | 25,945.7 ± 129.6 | 36,897.0 | 176,924.0 |
| `Modify` | `size=64` | 654.9 ± 0.9 | 1,491.4 ± 22.7 | 4,640.0 | 14,448.0 |
| `Modify` | `size=8` | 79.2 ± 0.1 | 209.3 ± 2.2 | 608.0 | 1,936.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 9.1 ± 0.7 | — | 0.0 | — |
| `DrillModify` | `-` | 83.8 ± 0.6 | — | 528.0 | — |
| `ServiceGet` | `-` | 3.6 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 61.5 ± 0.1 | — | 456.0 | — |
| `handDrillGet` | `-` | 12.2 ± 0.0 | — | 120.0 | — |
| `handDrillModify` | `-` | 77.7 ± 0.2 | — | 648.0 | — |
| `handServiceGet` | `-` | 12.2 ± 0.0 | — | 120.0 | — |
| `handServiceReplace` | `-` | 64.6 ± 0.1 | — | 576.0 | — |


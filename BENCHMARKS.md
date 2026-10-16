# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `66aa2024efcf236d91f01e2ca7ddcfce44477f51` · date: `2026-10-16` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.0 ± 0.1 | 9.7 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 26.7 ± 0.2 | 23.7 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 157.0 ± 1.8 | — | 720.0 | — |
| `ModifyCountry` | `-` | 322.0 ± 19.0 | — | 928.0 | — |
| `ModifyPartner` | `-` | 400.0 ± 7.4 | — | 1,000.0 | — |
| `ReadCountry` | `-` | 178.2 ± 7.3 | — | 520.0 | — |
| `ReadPartner` | `-` | 209.6 ± 3.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 310.1 ± 6.0 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,640.7 ± 9.9 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,901.6 ± 25.0 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,820.2 ± 24.4 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,648.9 ± 10.2 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,031.1 ± 51.7 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 947.0 ± 13.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 618.6 ± 7.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 403.7 ± 2.8 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 430.6 ± 5.3 | — | 1,549.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,373.4 ± 20.0 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,334.6 ± 22.0 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 53.7 ± 0.1 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 624.8 ± 8.6 | — | 568.0 | — |
| `handwritten_stream` | `-` | 158.1 ± 1.8 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 646.1 ± 8.8 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,944.5 ± 13.7 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,786.4 ± 25.8 | — | 4,000.0 | — |
| `WideToAvro` | `-` | 860.0 ± 8.4 | — | 2,040.0 | — |
| `WideToJson` | `-` | 679.0 ± 20.1 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,464.1 ± 14.7 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,966.6 ± 60.4 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 965.1 ± 3.4 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,885.4 ± 28.4 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 191.7 ± 1.0 | — | 832.0 | — |
| `decode_native` | `-` | 17.1 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 185.6 ± 0.9 | — | 832.0 | — |
| `encode_bridged` | `-` | 255.9 ± 4.2 | — | 1,256.0 | — |
| `encode_native` | `-` | 11.7 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 250.0 ± 5.7 | — | 1,240.0 | — |
| `fieldGet_bridged` | `-` | 100.6 ± 1.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 100.2 ± 0.9 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 394.1 ± 8.1 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 170.6 ± 6.6 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 23.2 ± 1.6 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 31.1 ± 2.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 32.9 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.0 ± 0.0 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 537.7 ± 6.7 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 520.8 ± 5.4 | — | 744.0 | — |
| `encode_hand` | `-` | 140.3 ± 0.5 | — | 768.0 | — |
| `encode_positional` | `-` | 6,315.0 ± 37.7 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,763.1 ± 102.9 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.3 ± 0.0 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 38.2 ± 0.4 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 20.9 ± 0.0 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.4 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 123.4 ± 0.4 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 57.4 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,231.9 ± 11.6 | 1,232.6 ± 7.7 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.4 ± 0.2 | 156.4 ± 0.9 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.4 ± 0.0 | 21.0 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,168.9 ± 32.1 | 2,162.1 ± 27.3 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 154.1 ± 0.4 | 154.5 ± 0.4 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.5 ± 0.1 | 26.1 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.1 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.7 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 23.0 ± 10.6 | 8.6 ± 0.6 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.5 ± 0.0 | 25.8 ± 1.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 404,908.3 ± 6,114.6 | — | 1,066,760.8 | — |
| `cModifyId` | `size=64` | 51,455.8 ± 832.0 | — | 136,293.6 | — |
| `cModifyId` | `size=8` | 8,497.8 ± 126.6 | — | 20,738.8 | — |
| `cReadId` | `size=512` | 204,663.8 ± 2,734.8 | — | 797,930.2 | — |
| `cReadId` | `size=64` | 25,825.3 ± 406.4 | — | 101,293.7 | — |
| `cReadId` | `size=8` | 4,048.3 ± 38.9 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 201,696.3 ± 2,582.2 | — | 797,929.4 | — |
| `cReadStreet` | `size=64` | 25,957.4 ± 775.8 | — | 101,293.9 | — |
| `cReadStreet` | `size=8` | 4,041.4 ± 83.3 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 405,083.5 ± 4,534.9 | — | 1,066,703.8 | — |
| `cReplaceId` | `size=64` | 50,860.7 ± 699.9 | — | 136,245.2 | — |
| `cReplaceId` | `size=8` | 8,571.2 ± 91.8 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 337,531.1 ± 6,918.9 | — | 1,240,738.2 | — |
| `cSumPrices` | `size=64` | 41,576.4 ± 460.5 | — | 157,067.7 | — |
| `cSumPrices` | `size=8` | 6,116.0 ± 96.6 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 178.2 ± 8.4 | — | 0.1 | — |
| `jMiss` | `size=64` | 179.1 ± 7.2 | — | 0.0 | — |
| `jMiss` | `size=8` | 167.8 ± 0.9 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,682.1 ± 17.9 | — | 41,921.0 | — |
| `jModifyId` | `size=64` | 468.2 ± 7.6 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 110.0 ± 1.3 | — | 984.0 | — |
| `jReadId` | `size=512` | 34.1 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.4 ± 2.4 | — | 56.0 | — |
| `jReadId` | `size=8` | 34.1 ± 0.6 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 197.0 ± 16.2 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 191.6 ± 4.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 188.8 ± 4.2 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,627.8 ± 10.4 | — | 41,889.0 | — |
| `jReplaceId` | `size=64` | 443.4 ± 2.6 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 107.1 ± 2.5 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 83,933.1 ± 684.3 | — | 63,662.3 | — |
| `jSumPrices` | `size=64` | 10,954.7 ± 815.6 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,434.5 ± 7.6 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 86.9 ± 2.1 | — | 320.0 | — |
| `MapDrillModify` | `-` | 38.9 ± 0.3 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 69.3 ± 5.4 | — | 216.0 | — |
| `handEnvUse` | `-` | 85.1 ± 0.7 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 33.8 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 62.0 ± 1.4 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 32.9 ± 0.5 | 29.8 ± 0.5 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,221.1 ± 730.7 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,147.6 ± 49.6 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 374.7 ± 2.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 84.6 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 49,792.7 ± 2,305.7 | — | 331,118.7 | — |
| `Modify_multiFocus` | `size=256` | 11,173.1 ± 29.7 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,357.3 ± 10.8 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 217.0 ± 2.6 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 34,504.2 ± 634.8 | — | 115,176.9 | — |
| `Modify_powerEach` | `size=256` | 8,211.4 ± 48.6 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,016.9 ± 10.3 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 199.4 ± 10.5 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,257.9 ± 35.1 | — | 65,578.1 | — |
| `naive_listMap` | `size=256` | 2,036.4 ± 13.0 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 248.3 ± 0.5 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.7 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,140.6 ± 50.6 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 723.4 ± 38.5 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.0 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.5 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.6 ± 0.2 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 122.6 ± 1.0 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.5 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.4 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.6 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.4 ± 0.2 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.9 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 156.4 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.4 ± 0.3 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,120.6 ± 16.8 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,059.2 ± 25.4 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.2 ± 0.0 | 21.5 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.2 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 58.2 ± 1.4 | 70.7 ± 0.6 | 160.0 | 304.0 |
| `Modify_6` | `-` | 135.3 ± 0.5 | 123.7 ± 5.5 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.6 ± 0.1 | 16.2 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.3 ± 0.0 | 6.9 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 33,362.9 ± 634.8 | — | 97,423.8 | — |
| `ModifyNames` | `size=64` | 3,942.4 ± 19.3 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 541.9 ± 5.7 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 110.9 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 110.8 ± 1.2 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 110.1 ± 0.4 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 34.9 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.0 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 90,799.6 ± 449.4 | — | 382,775.6 | — |
| `monocleModifyNames` | `size=64` | 8,867.5 ± 32.0 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,399.2 ± 26.6 | — | 5,405.3 | — |
| `monocleModifyStreet` | `size=512` | 45,816.0 ± 270.6 | — | 169,079.5 | — |
| `monocleModifyStreet` | `size=64` | 6,162.1 ± 277.9 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 922.5 ± 7.1 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 25,052.1 ± 143.2 | — | 69,783.4 | — |
| `monocleReadStreet` | `size=64` | 3,221.8 ± 16.0 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 475.4 ± 4.3 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,659.2 ± 909.6 | — | 226,296.1 | — |
| `naiveModifyNames` | `size=64` | 7,639.5 ± 48.2 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,085.3 ± 6.6 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,865.2 ± 458.9 | — | 169,056.7 | — |
| `naiveModifyStreet` | `size=64` | 5,934.8 ± 261.7 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 921.5 ± 10.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 24,960.1 ± 82.2 | — | 69,783.1 | — |
| `naiveReadStreet` | `size=64` | 3,296.1 ± 65.8 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 474.6 ± 4.6 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 242,628.2 ± 3,836.5 | — | 626,346.4 | — |
| `Names` | `size=64` | 30,602.1 ± 270.0 | — | 78,779.2 | — |
| `Names` | `size=8` | 4,254.0 ± 210.2 | — | 10,872.1 | — |
| `NamesIor` | `size=512` | 266,556.4 ± 1,775.9 | — | 691,765.6 | — |
| `NamesIor` | `size=64` | 33,264.0 ± 675.4 | — | 86,979.5 | — |
| `NamesIor` | `size=8` | 4,475.4 ± 37.6 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,052.8 ± 10.5 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,057.8 ± 5.9 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,074.8 ± 33.3 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,067.9 ± 27.7 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,047.9 ± 23.1 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,076.7 ± 22.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 238,146.2 ± 5,965.4 | — | 609,886.9 | — |
| `directNames` | `size=64` | 30,356.3 ± 690.5 | — | 77,707.5 | — |
| `directNames` | `size=8` | 4,117.3 ± 89.2 | — | 10,616.1 | — |
| `directStreet` | `size=512` | 1,039.5 ± 12.9 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,049.8 ± 30.2 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,056.2 ± 13.0 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 242,090.7 ± 2,147.0 | — | 605,786.0 | — |
| `hcursorNames` | `size=64` | 30,694.8 ± 604.4 | — | 77,793.3 | — |
| `hcursorNames` | `size=8` | 4,059.7 ± 23.2 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,124.6 ± 6.3 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,132.7 ± 28.7 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,129.8 ± 31.7 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 224,532.6 ± 2,456.0 | — | 1,121,758.9 | — |
| `monocleNames` | `size=64` | 23,736.8 ± 460.6 | — | 132,409.8 | — |
| `monocleNames` | `size=8` | 3,666.5 ± 31.2 | — | 19,461.4 | — |
| `monocleStreet` | `size=512` | 176,631.9 ± 1,373.4 | — | 908,028.0 | — |
| `monocleStreet` | `size=64` | 20,857.9 ± 295.7 | — | 113,471.2 | — |
| `monocleStreet` | `size=8` | 3,168.7 ± 25.9 | — | 17,058.7 | — |
| `naiveNames` | `size=512` | 194,663.8 ± 3,804.8 | — | 965,266.8 | — |
| `naiveNames` | `size=64` | 22,400.2 ± 378.8 | — | 120,476.7 | — |
| `naiveNames` | `size=8` | 3,344.5 ± 24.1 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 177,694.9 ± 3,648.0 | — | 908,028.7 | — |
| `naiveStreet` | `size=64` | 20,965.2 ± 144.4 | — | 113,785.9 | — |
| `naiveStreet` | `size=8` | 3,152.3 ± 5.3 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,901.8 ± 38.0 | — | 42,011.5 | — |
| `ModifyStreet` | `size=64` | 688.0 ± 52.3 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 293.3 ± 4.4 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 193.9 ± 0.8 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 196.1 ± 8.5 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 194.8 ± 8.0 | — | 109.3 | — |
| `SumPrices` | `size=512` | 85,975.3 ± 1,050.9 | — | 63,715.3 | — |
| `SumPrices` | `size=64` | 10,319.4 ± 68.9 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,438.6 ± 21.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 157,065.5 ± 1,470.7 | — | 333,570.6 | — |
| `monocleModifyStreet` | `size=64` | 19,034.4 ± 56.4 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,187.0 ± 117.0 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 87,037.3 ± 472.6 | — | 193,226.5 | — |
| `monocleReadStreet` | `size=64` | 11,227.2 ± 177.3 | — | 24,705.1 | — |
| `monocleReadStreet` | `size=8` | 1,787.3 ± 34.6 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 414,903.6 ± 2,226.2 | — | 1,178,436.0 | — |
| `monocleSumPrices` | `size=64` | 15,105.2 ± 245.1 | — | 45,841.5 | — |
| `monocleSumPrices` | `size=8` | 2,422.6 ± 17.3 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 156,207.1 ± 1,064.3 | — | 333,563.2 | — |
| `naiveModifyStreet` | `size=64` | 18,604.9 ± 193.3 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,138.0 ± 18.4 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 88,039.6 ± 757.9 | — | 193,227.4 | — |
| `naiveReadStreet` | `size=64` | 11,358.3 ± 76.2 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,784.8 ± 36.7 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 91,704.5 ± 500.7 | — | 230,118.5 | — |
| `naiveSumPrices` | `size=64` | 11,803.0 ± 161.3 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,861.6 ± 32.1 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 32,727.1 ± 398.6 | — | 451.4 | — |
| `nativeReadStreet` | `size=64` | 4,255.2 ± 91.2 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 803.3 ± 18.3 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 62,709.4 ± 1,389.9 | — | 86,271.0 | — |
| `nativeSumPrices` | `size=64` | 7,666.9 ± 85.1 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,149.7 ± 20.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 118,004.7 ± 669.1 | — | 624,381.9 | — |
| `TransformDeep` | `n=512` | 11,578.6 ± 125.7 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,415.5 ± 12.1 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 128,075.1 ± 640.2 | 162,598.8 ± 355.7 | 655,357.2 | 753,734.3 |
| `TransformExpr` | `n=512` | 15,943.0 ± 117.2 | 15,264.6 ± 87.9 | 81,825.6 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,987.2 ± 9.0 | 2,486.2 ± 15.9 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 103,613.9 ± 5,784.1 | — | 786,579.4 | — |
| `UniverseDeep` | `n=512` | 15,156.0 ± 85.2 | — | 98,377.5 | — |
| `UniverseDeep` | `n=64` | 1,855.5 ± 9.9 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 98,249.9 ± 2,792.5 | 2,896,761.9 ± 171,489.8 | 786,383.5 | 4,687,783.9 |
| `UniverseExpr` | `n=512` | 14,709.8 ± 65.4 | 200,043.2 ± 6,308.5 | 98,185.5 | 475,012.4 |
| `UniverseExpr` | `n=64` | 1,786.8 ± 5.7 | 18,400.2 ± 487.1 | 12,168.0 | 45,424.4 |
| `UniverseJson` | `n=4096` | 218,876.0 ± 2,400.0 | 3,271,465.6 ± 136,029.3 | 786,471.3 | 6,489,921.3 |
| `UniverseJson` | `n=512` | 24,575.7 ± 63.2 | 248,527.6 ± 8,999.2 | 98,186.5 | 699,921.4 |
| `UniverseJson` | `n=64` | 2,967.9 ± 4.5 | 22,336.9 ± 787.6 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 41,936.6 ± 193.6 | — | 163,886.5 | — |
| `visitorTransformDeep` | `n=512` | 4,881.5 ± 17.3 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 424.3 ± 0.6 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,209.3 ± 396.3 | — | 360,470.7 | — |
| `visitorTransformExpr` | `n=512` | 7,559.3 ± 43.2 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 993.9 ± 8.0 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,533.6 ± 696.5 | — | 196,703.7 | — |
| `visitorUniverseDeep` | `n=512` | 6,473.3 ± 50.7 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 742.6 ± 2.2 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,720.9 ± 439.9 | — | 196,654.4 | — |
| `visitorUniverseExpr` | `n=512` | 6,528.8 ± 71.8 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 796.6 ± 2.4 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 165,034.7 ± 5,551.3 | — | 447,928.1 | — |
| `visitorUniverseJson` | `n=512` | 24,674.1 ± 2,106.0 | — | 42,314.5 | — |
| `visitorUniverseJson` | `n=64` | 2,222.8 ± 130.5 | — | 5,768.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,202.3 ± 189.9 | — | 41,413.8 | — |
| `Modify_powerEach` | `size=16` | 255.9 ± 0.3 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,003.1 ± 36.6 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 124.4 ± 1.3 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,008.3 ± 1,986.7 | — | 164,371.1 | — |
| `Modify_powerEach` | `size=64` | 778.7 ± 8.3 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 59,897.7 ± 979.6 | — | 279,432.8 | — |
| `monocle_powerEach` | `size=16` | 601.6 ± 3.8 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,975.6 ± 1,412.8 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 209.6 ± 1.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 178,614.5 ± 3,612.1 | — | 967,842.8 | — |
| `monocle_powerEach` | `size=64` | 2,154.1 ± 25.3 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,669.3 ± 8.2 | — | 28,731.2 | — |
| `naive_powerEach` | `size=16` | 101.0 ± 0.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,608.1 ± 3.6 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 24.9 ± 0.0 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,652.0 ± 89.6 | — | 114,787.4 | — |
| `naive_powerEach` | `size=64` | 394.6 ± 1.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 72,508.7 ± 1,089.1 | — | 210,521.3 | — |
| `Modify_nested` | `size=16` | 1,463.1 ± 12.2 | — | 4,792.1 | — |
| `Modify_nested` | `size=256` | 17,412.6 ± 175.9 | — | 53,784.2 | — |
| `Modify_nested` | `size=4` | 686.0 ± 17.6 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,565.7 ± 30.1 | — | 14,693.9 | — |
| `monocle_nested` | `size=1024` | 247,647.7 ± 2,502.4 | — | 1,118,850.9 | — |
| `monocle_nested` | `size=16` | 2,653.9 ± 35.5 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 94,373.6 ± 2,606.0 | — | 430,209.6 | — |
| `monocle_nested` | `size=4` | 1,304.9 ± 111.5 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,954.8 ± 80.8 | — | 58,833.8 | — |
| `naive_nested` | `size=1024` | 21,610.2 ± 180.0 | — | 115,071.4 | — |
| `naive_nested` | `size=16` | 363.3 ± 3.0 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,214.6 ± 22.7 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 125.6 ± 2.5 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,314.7 ± 15.5 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,391.0 ± 118.3 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 22,771.7 ± 141.0 | — | 104,675.2 | — |
| `Modify_sparse` | `size=32` | 351.8 ± 10.5 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,681.6 ± 51.0 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 118.9 ± 0.9 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,841.0 ± 8.3 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 106,117.1 ± 1,149.0 | — | 523,146.0 | — |
| `monocle_sparse` | `size=32` | 1,057.9 ± 19.1 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,420.1 ± 636.2 | — | 166,685.8 | — |
| `monocle_sparse` | `size=8` | 343.3 ± 9.6 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 285.0 ± 0.4 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,894.1 ± 31.6 | — | 24,612.2 | — |
| `naive_sparse` | `size=32` | 73.7 ± 0.1 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,156.1 ± 6.7 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 24.9 ± 0.3 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.4 ± 0.2 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.3 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 33.8 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.5 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.9 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 95,522.5 ± 826.8 | — | 589,712.7 | — |
| `Cata` | `-` | 68,999.1 ± 1,199.4 | — | 197,568.5 | — |
| `Hylo` | `-` | 89,879.7 ± 677.8 | — | 295,848.6 | — |
| `drosteAna` | `-` | 45,949.9 ± 281.6 | — | 327,632.3 | — |
| `drosteCata` | `-` | 37,878.3 ± 144.5 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 63,273.0 ± 1,477.7 | — | 328,640.4 | — |
| `handAna` | `-` | 20,398.5 ± 143.0 | — | 163,816.1 | — |
| `handCata` | `-` | 12,596.6 ± 37.7 | — | 0.1 | — |
| `handHylo` | `-` | 9,500.5 ± 13.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.2 ± 0.0 | 27.9 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.6 ± 0.2 | 64.2 ± 0.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.2 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,995.1 ± 69.9 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 529.8 ± 2.8 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 51.5 ± 4.2 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,882.6 ± 26.5 | 30,941.1 ± 277.5 | 12,312.3 | 162,564.9 |
| `FoldPrices` | `size=64` | 153.7 ± 0.4 | 2,067.3 ± 14.9 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.6 ± 0.1 | 323.9 ± 2.0 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,285.4 ± 165.8 | 33,162.7 ± 327.8 | 36,897.2 | 176,890.6 |
| `Modify` | `size=64` | 836.4 ± 1.2 | 1,780.6 ± 12.9 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 94.6 ± 0.6 | 284.0 ± 1.6 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.3 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 92.6 ± 0.7 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.3 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 65.6 ± 0.8 | — | 400.0 | — |
| `handDrillGet` | `-` | 13.9 ± 0.0 | — | 120.0 | — |
| `handDrillModify` | `-` | 89.8 ± 0.7 | — | 592.0 | — |
| `handServiceGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 76.7 ± 4.2 | — | 520.0 | — |


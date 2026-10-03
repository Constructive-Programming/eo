# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `462e037b7df74c393ecb5ad4a2d0b8b7d2b4bf96` · date: `2026-10-03` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.8 ± 0.0 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.7 ± 0.1 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.8 ± 0.1 | 0.7 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 9.2 ± 0.1 | 6.2 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 18.7 ± 0.1 | 16.3 ± 0.4 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 0.8 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 110.1 ± 0.2 | — | 720.0 | — |
| `ModifyCountry` | `-` | 236.9 ± 4.9 | — | 928.0 | — |
| `ModifyPartner` | `-` | 278.2 ± 1.3 | — | 984.0 | — |
| `ReadCountry` | `-` | 133.6 ± 2.6 | — | 520.0 | — |
| `ReadPartner` | `-` | 144.9 ± 1.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 211.9 ± 2.2 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,131.4 ± 20.6 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,513.9 ± 49.1 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,091.2 ± 30.9 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,278.6 ± 11.6 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,638.3 ± 45.1 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 772.3 ± 12.6 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 538.2 ± 5.4 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 274.0 ± 1.4 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 312.0 ± 5.6 | — | 1,541.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,161.3 ± 21.7 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,090.7 ± 11.9 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 48.4 ± 0.5 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 448.6 ± 11.6 | — | 568.0 | — |
| `handwritten_stream` | `-` | 143.6 ± 6.8 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 704.5 ± 7.2 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,276.8 ± 14.4 | — | 4,674.7 | — |
| `ClickToJson` | `-` | 2,138.1 ± 28.8 | — | 3,962.7 | — |
| `WideToAvro` | `-` | 650.7 ± 7.4 | — | 2,024.0 | — |
| `WideToJson` | `-` | 472.3 ± 13.0 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,314.4 ± 20.9 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,322.9 ± 27.8 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 963.2 ± 10.1 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,427.4 ± 10.5 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 141.5 ± 0.8 | — | 832.0 | — |
| `decode_native` | `-` | 11.3 ± 0.0 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 139.2 ± 0.5 | — | 832.0 | — |
| `encode_bridged` | `-` | 182.2 ± 1.2 | — | 1,224.0 | — |
| `encode_native` | `-` | 10.0 ± 0.3 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 183.1 ± 2.2 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 77.1 ± 1.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 78.0 ± 2.1 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 274.3 ± 5.7 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 119.8 ± 1.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 16.6 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 17.7 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 16.7 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 16.5 ± 0.2 | — | 0.0 | — |
| `getCap` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 5.6 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 27.7 ± 0.5 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 26.1 ± 0.1 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.7 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.6 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.6 ± 0.1 | — | 40.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 379.8 ± 9.7 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 392.3 ± 2.8 | — | 744.0 | — |
| `encode_hand` | `-` | 113.6 ± 0.7 | — | 768.0 | — |
| `encode_positional` | `-` | 4,302.6 ± 54.9 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 5,153.7 ± 101.9 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 10.2 ± 0.2 | — | 72.0 | — |
| `buildLens3` | `-` | 27.0 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 51.9 ± 0.5 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 27.0 ± 0.2 | — | 184.0 | — |
| `reuseLeaf` | `-` | 3.5 ± 0.1 | — | 24.0 | — |
| `reuseLens1` | `-` | 10.3 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 28.3 ± 0.3 | — | 72.0 | — |
| `reuseLens6` | `-` | 77.5 ± 0.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 38.8 ± 0.2 | — | 160.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,303.8 ± 12.0 | 1,288.9 ± 10.5 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 138.2 ± 3.5 | 134.8 ± 1.7 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 16.6 ± 0.1 | 16.3 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,278.8 ± 27.0 | 2,276.3 ± 23.8 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 219.7 ± 2.5 | 217.5 ± 2.0 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 29.9 ± 0.1 | 29.7 ± 0.5 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.3 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 10.2 ± 0.1 | 5.3 ± 0.3 | 0.0 | 0.0 |
| `Get_6` | `-` | 19.6 ± 0.1 | 16.7 ± 0.2 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.7 ± 0.0 | 0.4 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.5 ± 0.0 | 4.6 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 4.4 ± 0.0 | 4.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 281,082.8 ± 2,777.1 | — | 1,066,680.0 | — |
| `cModifyId` | `size=64` | 35,897.2 ± 250.9 | — | 136,264.5 | — |
| `cModifyId` | `size=8` | 6,220.7 ± 125.7 | — | 20,704.1 | — |
| `cReadId` | `size=512` | 148,919.7 ± 1,886.6 | — | 797,914.4 | — |
| `cReadId` | `size=64` | 18,725.8 ± 155.7 | — | 101,288.5 | — |
| `cReadId` | `size=8` | 2,912.1 ± 13.9 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 149,872.7 ± 2,125.4 | — | 797,914.6 | — |
| `cReadStreet` | `size=64` | 18,757.4 ± 165.8 | — | 101,288.5 | — |
| `cReadStreet` | `size=8` | 2,963.1 ± 50.7 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 281,300.1 ± 3,052.2 | — | 1,066,616.0 | — |
| `cReplaceId` | `size=64` | 36,251.0 ± 716.8 | — | 136,216.6 | — |
| `cReplaceId` | `size=8` | 6,053.6 ± 27.4 | — | 20,656.1 | — |
| `cSumPrices` | `size=512` | 243,908.2 ± 2,294.7 | — | 1,240,709.4 | — |
| `cSumPrices` | `size=64` | 30,276.8 ± 111.4 | — | 157,486.9 | — |
| `cSumPrices` | `size=8` | 4,616.8 ± 34.5 | — | 22,720.0 | — |
| `jMiss` | `size=512` | 143.2 ± 3.0 | — | 0.0 | — |
| `jMiss` | `size=64` | 142.7 ± 2.6 | — | 0.0 | — |
| `jMiss` | `size=8` | 144.9 ± 1.1 | — | 0.0 | — |
| `jModifyId` | `size=512` | 5,342.7 ± 120.9 | — | 41,921.5 | — |
| `jModifyId` | `size=64` | 669.0 ± 14.3 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 146.1 ± 2.3 | — | 992.0 | — |
| `jReadId` | `size=512` | 32.7 ± 4.3 | — | 64.0 | — |
| `jReadId` | `size=64` | 30.5 ± 5.5 | — | 48.0 | — |
| `jReadId` | `size=8` | 32.9 ± 4.3 | — | 64.0 | — |
| `jReadStreet` | `size=512` | 145.7 ± 1.2 | — | 136.0 | — |
| `jReadStreet` | `size=64` | 147.5 ± 1.8 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 148.1 ± 1.4 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 5,295.2 ± 74.1 | — | 41,889.5 | — |
| `jReplaceId` | `size=64` | 650.6 ± 10.3 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 143.8 ± 0.9 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 67,706.8 ± 571.7 | — | 63,671.3 | — |
| `jSumPrices` | `size=64` | 8,504.5 ± 106.3 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,132.3 ± 15.7 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 83.8 ± 1.0 | — | 320.0 | — |
| `MapDrillModify` | `-` | 34.4 ± 0.4 | — | 216.0 | — |
| `MapGet` | `-` | 1.8 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 66.4 ± 2.9 | — | 216.0 | — |
| `handEnvUse` | `-` | 82.5 ± 1.2 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 33.0 ± 0.2 | — | 216.0 | — |
| `handMapGet` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 65.0 ± 3.0 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.6 ± 0.1 | 5.6 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 26.1 ± 0.1 | 26.9 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 5.5 ± 0.1 | 5.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 15,494.7 ± 1,970.6 | — | 43,036.0 | — |
| `Fold_powerEach` | `size=256` | 2,638.1 ± 10.2 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 273.9 ± 0.8 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 56.7 ± 0.2 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 56,453.4 ± 929.3 | — | 331,134.5 | — |
| `Modify_multiFocus` | `size=256` | 12,672.1 ± 123.8 | — | 76,112.9 | — |
| `Modify_multiFocus` | `size=32` | 1,461.3 ± 5.3 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 190.7 ± 0.8 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 29,220.8 ± 746.2 | — | 115,175.6 | — |
| `Modify_powerEach` | `size=256` | 6,024.9 ± 474.2 | — | 26,080.4 | — |
| `Modify_powerEach` | `size=32` | 758.1 ± 15.6 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 129.5 ± 1.4 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 10,123.5 ± 28.1 | — | 65,578.6 | — |
| `naive_listMap` | `size=256` | 2,480.4 ± 10.5 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 314.7 ± 1.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 45.0 ± 0.5 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,592.6 ± 176.5 | — | 16,129.2 | — |
| `naive_sumQty` | `size=256` | 607.1 ± 2.3 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 51.7 ± 0.1 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 5.4 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 24.2 ± 0.1 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 129.0 ± 0.5 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 18.9 ± 0.4 | — | 128.0 | — |
| `Modify_multiFocusTuple6` | `-` | 33.2 ± 0.2 | — | 224.0 | — |
| `naive_constSum` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 22.5 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 13.0 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 24.5 ± 0.2 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 203.8 ± 0.9 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 36.1 ± 0.0 | — | 184.0 | — |
| `buildAndUse` | `-` | 799.2 ± 24.3 | — | 2,840.0 | — |
| `reuseUse` | `-` | 757.2 ± 11.1 | — | 2,696.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 17.9 ± 0.1 | 17.9 ± 0.1 | 112.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.1 ± 0.0 | 1.3 ± 0.1 | 0.0 | 0.0 |
| `Modify_3` | `-` | 38.5 ± 0.4 | 51.4 ± 0.3 | 160.0 | 304.0 |
| `Modify_6` | `-` | 89.3 ± 0.4 | 84.6 ± 1.5 | 208.0 | 496.0 |
| `Modify_loyalty` | `-` | 17.0 ± 0.2 | 16.9 ± 0.1 | 112.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 5.7 ± 0.0 | 5.5 ± 0.1 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 11.9 ± 0.1 | 11.8 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 25,112.6 ± 137.5 | — | 97,421.9 | — |
| `ModifyNames` | `size=64` | 3,014.1 ± 58.4 | — | 13,128.1 | — |
| `ModifyNames` | `size=8` | 453.9 ± 1.2 | — | 2,160.0 | — |
| `ModifyStreet` | `size=512` | 86.3 ± 0.2 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 85.5 ± 0.2 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 86.1 ± 1.3 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 24.5 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 24.3 ± 0.0 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 24.4 ± 0.1 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 71,679.5 ± 377.2 | — | 382,786.6 | — |
| `monocleModifyNames` | `size=64` | 7,420.1 ± 79.9 | — | 39,840.3 | — |
| `monocleModifyNames` | `size=8` | 1,058.5 ± 8.4 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 33,901.1 ± 261.0 | — | 169,070.7 | — |
| `monocleModifyStreet` | `size=64` | 4,412.2 ± 77.4 | — | 20,904.1 | — |
| `monocleModifyStreet` | `size=8` | 689.2 ± 22.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 17,105.2 ± 29.6 | — | 69,780.1 | — |
| `monocleReadStreet` | `size=64` | 2,187.9 ± 23.5 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 329.4 ± 4.4 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 43,850.4 ± 481.5 | — | 226,287.6 | — |
| `naiveModifyNames` | `size=64` | 5,631.5 ± 66.1 | — | 27,936.2 | — |
| `naiveModifyNames` | `size=8` | 818.2 ± 15.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 33,797.8 ± 421.3 | — | 169,046.7 | — |
| `naiveModifyStreet` | `size=64` | 4,427.2 ± 83.5 | — | 20,880.1 | — |
| `naiveModifyStreet` | `size=8` | 694.7 ± 19.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 17,173.6 ± 241.4 | — | 69,780.1 | — |
| `naiveReadStreet` | `size=64` | 2,180.4 ± 3.7 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 328.9 ± 3.6 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 165,861.5 ± 3,391.9 | — | 613,973.0 | — |
| `Names` | `size=64` | 21,094.6 ± 536.0 | — | 78,778.2 | — |
| `Names` | `size=8` | 2,870.5 ± 59.8 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 170,538.4 ± 3,253.5 | — | 679,376.7 | — |
| `NamesIor` | `size=64` | 23,127.9 ± 330.3 | — | 86,962.4 | — |
| `NamesIor` | `size=8` | 3,111.5 ± 107.2 | — | 11,576.1 | — |
| `Street` | `size=512` | 748.1 ± 11.6 | — | 2,720.6 | — |
| `Street` | `size=64` | 755.7 ± 19.1 | — | 2,720.1 | — |
| `Street` | `size=8` | 771.6 ± 11.8 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 779.5 ± 7.8 | — | 2,736.6 | — |
| `StreetIor` | `size=64` | 771.1 ± 23.8 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 761.4 ± 17.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 160,671.7 ± 3,762.5 | — | 613,928.8 | — |
| `directNames` | `size=64` | 20,432.2 ± 275.8 | — | 77,698.2 | — |
| `directNames` | `size=8` | 2,801.1 ± 78.1 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 770.9 ± 7.7 | — | 2,736.6 | — |
| `directStreet` | `size=64` | 768.2 ± 3.8 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 772.4 ± 8.7 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 160,016.3 ± 5,165.1 | — | 613,928.3 | — |
| `hcursorNames` | `size=64` | 20,488.9 ± 691.2 | — | 77,778.2 | — |
| `hcursorNames` | `size=8` | 2,789.6 ± 146.3 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 810.9 ± 8.2 | — | 3,032.6 | — |
| `hcursorStreet` | `size=64` | 801.4 ± 10.2 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 810.7 ± 8.6 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 180,538.0 ± 2,074.2 | — | 1,121,729.3 | — |
| `monocleNames` | `size=64` | 20,767.7 ± 434.0 | — | 132,756.5 | — |
| `monocleNames` | `size=8` | 3,184.3 ± 24.0 | — | 19,466.7 | — |
| `monocleStreet` | `size=512` | 140,475.5 ± 2,884.0 | — | 907,998.4 | — |
| `monocleStreet` | `size=64` | 17,668.2 ± 335.9 | — | 113,809.6 | — |
| `monocleStreet` | `size=8` | 2,753.2 ± 34.7 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 147,247.6 ± 1,961.3 | — | 965,235.0 | — |
| `naiveNames` | `size=64` | 18,519.0 ± 231.9 | — | 120,825.7 | — |
| `naiveNames` | `size=8` | 2,882.6 ± 48.1 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 139,044.2 ± 553.9 | — | 908,008.1 | — |
| `naiveStreet` | `size=64` | 17,586.3 ± 160.9 | — | 113,774.9 | — |
| `naiveStreet` | `size=8` | 2,724.6 ± 26.7 | — | 17,034.7 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 5,605.8 ± 58.5 | — | 42,029.0 | — |
| `ModifyStreet` | `size=64` | 803.2 ± 4.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 233.2 ± 1.1 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 147.4 ± 2.1 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 149.7 ± 1.2 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 147.2 ± 2.5 | — | 90.7 | — |
| `SumPrices` | `size=512` | 67,758.6 ± 332.5 | — | 63,713.1 | — |
| `SumPrices` | `size=64` | 8,401.5 ± 64.8 | — | 8,121.0 | — |
| `SumPrices` | `size=8` | 1,147.9 ± 22.4 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 119,210.4 ± 699.3 | — | 333,548.1 | — |
| `monocleModifyStreet` | `size=64` | 14,249.5 ± 30.7 | — | 30,081.4 | — |
| `monocleModifyStreet` | `size=8` | 2,516.6 ± 21.3 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 62,735.4 ± 378.6 | — | 193,205.7 | — |
| `monocleReadStreet` | `size=64` | 8,035.7 ± 145.8 | — | 24,704.8 | — |
| `monocleReadStreet` | `size=8` | 1,277.8 ± 12.7 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 295,494.7 ± 2,715.7 | — | 1,178,332.9 | — |
| `monocleSumPrices` | `size=64` | 11,464.7 ± 31.9 | — | 45,833.2 | — |
| `monocleSumPrices` | `size=8` | 1,882.8 ± 14.7 | — | 6,338.7 | — |
| `naiveModifyStreet` | `size=512` | 119,443.0 ± 445.5 | — | 333,499.9 | — |
| `naiveModifyStreet` | `size=64` | 14,268.9 ± 134.0 | — | 30,057.4 | — |
| `naiveModifyStreet` | `size=8` | 2,500.8 ± 4.7 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 62,951.2 ± 232.8 | — | 193,205.9 | — |
| `naiveReadStreet` | `size=64` | 7,949.0 ± 18.8 | — | 24,704.8 | — |
| `naiveReadStreet` | `size=8` | 1,271.5 ± 6.1 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 68,711.6 ± 442.1 | — | 230,098.9 | — |
| `naiveSumPrices` | `size=64` | 8,578.0 ± 21.8 | — | 29,336.9 | — |
| `naiveSumPrices` | `size=8` | 1,348.2 ± 6.6 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 28,950.9 ± 219.7 | — | 448.2 | — |
| `nativeReadStreet` | `size=64` | 3,734.5 ± 69.9 | — | 424.4 | — |
| `nativeReadStreet` | `size=8` | 667.3 ± 8.2 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 47,023.9 ± 989.3 | — | 86,269.7 | — |
| `nativeSumPrices` | `size=64` | 5,744.5 ± 15.2 | — | 10,920.6 | — |
| `nativeSumPrices` | `size=8` | 856.3 ± 5.9 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 113,589.8 ± 270.8 | — | 624,378.7 | — |
| `TransformDeep` | `n=512` | 12,073.9 ± 177.3 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,262.8 ± 4.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 114,650.3 ± 2,459.4 | 134,018.3 ± 1,604.5 | 655,347.5 | 753,713.5 |
| `TransformExpr` | `n=512` | 14,224.3 ± 42.5 | 12,097.4 ± 92.1 | 81,825.5 | 69,585.2 |
| `TransformExpr` | `n=64` | 1,748.8 ± 20.5 | 2,043.7 ± 12.4 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 139,189.4 ± 1,415.4 | — | 786,605.3 | — |
| `UniverseDeep` | `n=512` | 16,866.2 ± 62.4 | — | 98,377.7 | — |
| `UniverseDeep` | `n=64` | 1,945.7 ± 6.2 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 137,192.7 ± 991.7 | 4,297,983.2 ± 173,922.3 | 786,411.9 | 4,688,802.6 |
| `UniverseExpr` | `n=512` | 16,558.9 ± 210.1 | 301,081.2 ± 5,228.7 | 98,185.7 | 475,022.7 |
| `UniverseExpr` | `n=64` | 1,904.5 ± 9.4 | 26,311.1 ± 151.7 | 12,168.0 | 45,424.5 |
| `UniverseJson` | `n=4096` | 178,734.8 ± 1,039.6 | 4,529,430.0 ± 214,793.4 | 786,442.1 | 6,884,001.3 |
| `UniverseJson` | `n=512` | 21,362.6 ± 81.5 | 331,801.0 ± 4,258.1 | 98,186.2 | 699,929.9 |
| `UniverseJson` | `n=64` | 2,387.4 ± 45.3 | 30,140.9 ± 164.7 | 12,168.0 | 73,208.6 |
| `visitorTransformDeep` | `n=4096` | 39,532.5 ± 203.4 | — | 163,884.8 | — |
| `visitorTransformDeep` | `n=512` | 4,360.8 ± 80.5 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 484.5 ± 4.0 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 61,466.4 ± 176.0 | — | 360,468.7 | — |
| `visitorTransformExpr` | `n=512` | 7,196.9 ± 17.8 | — | 45,032.7 | — |
| `visitorTransformExpr` | `n=64` | 938.6 ± 3.6 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 52,905.9 ± 213.8 | — | 196,702.5 | — |
| `visitorUniverseDeep` | `n=512` | 6,432.1 ± 20.5 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 662.8 ± 1.3 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 46,975.4 ± 178.3 | — | 196,650.2 | — |
| `visitorUniverseExpr` | `n=512` | 5,799.4 ± 32.8 | — | 24,584.6 | — |
| `visitorUniverseExpr` | `n=64` | 681.0 ± 8.8 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 111,847.0 ± 12,983.9 | — | 299,674.6 | — |
| `visitorUniverseJson` | `n=512` | 18,126.2 ± 1,237.6 | — | 42,313.9 | — |
| `visitorUniverseJson` | `n=64` | 1,670.9 ± 82.8 | — | 4,424.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 10,606.2 ± 32.4 | — | 41,413.0 | — |
| `Modify_powerEach` | `size=16` | 180.3 ± 1.2 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,462.5 ± 13.8 | — | 10,688.4 | — |
| `Modify_powerEach` | `size=4` | 91.5 ± 0.2 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 42,276.9 ± 1,158.2 | — | 164,356.9 | — |
| `Modify_powerEach` | `size=64` | 657.0 ± 12.5 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 50,383.4 ± 666.2 | — | 279,426.2 | — |
| `monocle_powerEach` | `size=16` | 595.1 ± 2.4 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 18,151.2 ± 91.3 | — | 107,330.8 | — |
| `monocle_powerEach` | `size=4` | 167.2 ± 1.6 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 176,221.2 ± 2,120.9 | — | 967,839.0 | — |
| `monocle_powerEach` | `size=64` | 2,226.2 ± 11.8 | — | 14,514.7 | — |
| `naive_powerEach` | `size=1024` | 5,284.5 ± 182.9 | — | 28,730.5 | — |
| `naive_powerEach` | `size=16` | 84.7 ± 0.9 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,246.1 ± 5.7 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 26.2 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 20,329.7 ± 97.9 | — | 114,777.1 | — |
| `naive_powerEach` | `size=64` | 318.4 ± 2.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 51,294.5 ± 907.5 | — | 210,560.0 | — |
| `Modify_nested` | `size=16` | 1,103.3 ± 34.1 | — | 4,792.0 | — |
| `Modify_nested` | `size=256` | 14,889.8 ± 308.2 | — | 53,729.4 | — |
| `Modify_nested` | `size=4` | 527.3 ± 11.0 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,018.3 ± 424.4 | — | 14,640.5 | — |
| `monocle_nested` | `size=1024` | 207,300.7 ± 2,134.2 | — | 1,118,777.4 | — |
| `monocle_nested` | `size=16` | 2,612.0 ± 91.6 | — | 15,749.4 | — |
| `monocle_nested` | `size=256` | 75,908.4 ± 724.0 | — | 430,204.5 | — |
| `monocle_nested` | `size=4` | 1,026.8 ± 52.2 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 9,280.5 ± 95.8 | — | 58,859.7 | — |
| `naive_nested` | `size=1024` | 19,638.8 ± 112.7 | — | 115,067.8 | — |
| `naive_nested` | `size=16` | 353.8 ± 6.1 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 4,959.0 ± 67.9 | — | 29,019.1 | — |
| `naive_nested` | `size=4` | 126.8 ± 1.0 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,247.3 ± 23.9 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,141.5 ± 17.6 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 19,349.0 ± 231.6 | — | 104,672.0 | — |
| `Modify_sparse` | `size=32` | 276.7 ± 2.8 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 4,936.7 ± 51.6 | — | 24,785.4 | — |
| `Modify_sparse` | `size=8` | 92.0 ± 1.2 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,281.4 ± 34.8 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 94,378.5 ± 1,613.7 | — | 523,141.3 | — |
| `monocle_sparse` | `size=32` | 1,104.9 ± 8.5 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 29,093.4 ± 401.3 | — | 166,681.6 | — |
| `monocle_sparse` | `size=8` | 322.3 ± 7.6 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 277.9 ± 4.8 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,417.9 ± 47.3 | — | 24,611.8 | — |
| `naive_sparse` | `size=32` | 72.6 ± 0.5 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,167.8 ± 13.4 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 20.4 ± 0.3 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.7 ± 0.0 | 0.8 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 0.8 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.4 ± 0.0 | 2.5 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 13.2 ± 0.1 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 22.7 ± 0.4 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 10.3 ± 0.4 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 16.7 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 97,638.1 ± 1,816.4 | — | 589,712.7 | — |
| `Cata` | `-` | 56,898.7 ± 493.8 | — | 197,568.4 | — |
| `Hylo` | `-` | 66,291.2 ± 853.8 | — | 295,848.5 | — |
| `drosteAna` | `-` | 51,618.6 ± 441.7 | — | 327,632.4 | — |
| `drosteCata` | `-` | 31,011.1 ± 162.5 | — | 164,824.2 | — |
| `drosteHylo` | `-` | 52,346.2 ± 987.2 | — | 328,640.4 | — |
| `handAna` | `-` | 27,962.2 ± 236.1 | — | 163,816.2 | — |
| `handCata` | `-` | 12,707.1 ± 130.0 | — | 0.1 | — |
| `handHylo` | `-` | 10,531.1 ± 379.5 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.4 ± 0.0 | 3.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.4 ± 0.1 | 26.6 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 20.2 ± 0.3 | 49.5 ± 0.4 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 5.4 ± 0.1 | 5.4 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,674.9 ± 52.6 | — | 20,200.7 | — |
| `FoldNested` | `size=64` | 534.3 ± 9.2 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 54.2 ± 6.9 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,986.2 ± 42.9 | 27,737.5 ± 201.9 | 12,312.3 | 162,564.4 |
| `FoldPrices` | `size=64` | 221.2 ± 4.5 | 2,355.4 ± 9.4 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 30.1 ± 0.2 | 356.1 ± 4.8 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,270.3 ± 69.4 | 30,282.1 ± 220.1 | 36,897.2 | 176,888.1 |
| `Modify` | `size=64` | 815.8 ± 14.2 | 2,192.6 ± 21.4 | 4,640.0 | 14,408.1 |
| `Modify` | `size=8` | 97.7 ± 2.0 | 309.9 ± 4.6 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 7.4 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 86.4 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 4.1 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 75.3 ± 3.8 | — | 400.0 | — |
| `handDrillGet` | `-` | 17.5 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 94.3 ± 0.4 | — | 592.0 | — |
| `handServiceGet` | `-` | 17.5 ± 0.3 | — | 120.0 | — |
| `handServiceReplace` | `-` | 84.2 ± 0.2 | — | 520.0 | — |


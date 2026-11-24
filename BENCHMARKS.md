# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `edaa884baaf1c2a4e1be332787ca95fa6af46b12` · date: `2026-11-24` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.5 ± 0.1 | 9.8 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 28.7 ± 0.2 | 23.9 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 157.8 ± 3.9 | — | 720.0 | — |
| `ModifyCountry` | `-` | 306.6 ± 7.5 | — | 928.0 | — |
| `ModifyPartner` | `-` | 381.1 ± 5.0 | — | 984.0 | — |
| `ReadCountry` | `-` | 178.0 ± 6.1 | — | 520.0 | — |
| `ReadPartner` | `-` | 209.0 ± 3.3 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 304.5 ± 3.2 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,581.3 ± 40.5 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,931.0 ± 25.1 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,881.7 ± 45.7 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,632.0 ± 41.6 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,018.4 ± 21.4 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 938.9 ± 12.0 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 611.3 ± 6.7 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 402.5 ± 4.0 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 435.5 ± 4.9 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,327.5 ± 36.7 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,306.6 ± 49.4 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 53.6 ± 0.4 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 607.2 ± 10.9 | — | 568.0 | — |
| `handwritten_stream` | `-` | 158.6 ± 1.3 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 636.0 ± 48.8 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,830.3 ± 59.7 | — | 4,541.4 | — |
| `ClickToJson` | `-` | 2,534.4 ± 32.0 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 849.2 ± 7.4 | — | 2,040.0 | — |
| `WideToJson` | `-` | 683.2 ± 8.5 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,344.4 ± 14.5 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,609.7 ± 25.1 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 921.0 ± 14.5 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,760.2 ± 19.9 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 365.5 ± 4.6 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 313.5 ± 11.4 | — | 629.4 | — |
| `circeStrictFull` | `-` | 374.3 ± 12.4 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 697.9 ± 29.3 | — | 2,474.7 | — |
| `jsoniterDefaultsFull` | `-` | 1,173.6 ± 13.7 | — | 4,192.0 | — |
| `jsoniterDefaultsSparse` | `-` | 314.8 ± 3.6 | — | 704.0 | — |
| `jsoniterStrictFull` | `-` | 1,146.3 ± 49.1 | — | 4,192.0 | — |
| `jsoniterStrictSparse` | `-` | 472.3 ± 5.4 | — | 2,888.0 | — |
| `jsoniterWithDefaults` | `-` | 707.8 ± 12.6 | — | 984.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 192.8 ± 4.9 | — | 832.0 | — |
| `decode_native` | `-` | 17.4 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 184.9 ± 0.9 | — | 832.0 | — |
| `encode_bridged` | `-` | 254.1 ± 6.0 | — | 1,256.0 | — |
| `encode_native` | `-` | 12.1 ± 0.5 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 244.4 ± 8.2 | — | 1,240.0 | — |
| `fieldGet_bridged` | `-` | 100.4 ± 2.2 | — | 432.0 | — |
| `fieldGet_native` | `-` | 100.7 ± 1.8 | — | 437.3 | — |
| `rootGet_bridged` | `-` | 401.8 ± 9.4 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 171.6 ± 7.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.4 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.1 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.6 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.0 ± 0.3 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.0 ± 0.0 | — | 40.0 | — |
| `modifyFCap` | `-` | 8.3 ± 0.1 | — | 56.0 | — |
| `modifyFDerivedHeld` | `-` | 15.3 ± 0.1 | — | 56.0 | — |
| `modifyFDerivedPerCall` | `-` | 15.3 ± 0.9 | — | 56.0 | — |
| `modifyFDirect` | `-` | 8.0 ± 0.1 | — | 56.0 | — |
| `modifyFEachDerivedHeld` | `-` | 120.1 ± 3.2 | — | 432.0 | — |
| `modifyFEachDerivedPerCall` | `-` | 121.7 ± 1.1 | — | 472.0 | — |
| `modifyFEachDirect` | `-` | 118.1 ± 2.3 | — | 448.0 | — |
| `modifyFEachExt` | `-` | 122.5 ± 1.3 | — | 472.0 | — |
| `modifyFExt` | `-` | 14.9 ± 0.1 | — | 56.0 | — |
| `modifyFModifyDirect` | `-` | 5.3 ± 0.1 | — | 40.0 | — |
| `modifyFModifyGeneric` | `-` | 30.0 ± 0.4 | — | 224.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.8 ± 0.4 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 24.1 ± 0.3 | — | 160.0 | — |
| `GetterRead` | `-` | 13.5 ± 0.1 | — | 48.0 | — |
| `Patch` | `-` | 6.3 ± 0.0 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 15.8 ± 0.3 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 20.8 ± 0.1 | — | 160.0 | — |
| `TransformFocus` | `-` | 821.8 ± 6.3 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,133.1 ± 7.0 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 892.3 ± 7.0 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.3 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 22.7 ± 0.2 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 22.9 ± 0.2 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.1 ± 0.2 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.5 ± 0.3 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.6 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.4 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.9 ± 0.0 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.3 ± 0.2 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 807.4 ± 3.0 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 955.4 ± 6.3 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,413.1 ± 127.8 | — | 10,578.7 | — |
| `chimneyTransformFocusWrite` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.6 ± 0.2 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.4 ± 0.1 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.7 ± 0.2 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.3 ± 0.1 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.2 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 411.6 ± 3.3 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 385.4 ± 2.2 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 263.2 ± 2.1 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.7 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.5 ± 0.2 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.6 ± 0.0 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 538.7 ± 24.5 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 535.2 ± 10.9 | — | 744.0 | — |
| `encode_hand` | `-` | 140.2 ± 1.7 | — | 768.0 | — |
| `encode_positional` | `-` | 6,274.7 ± 63.6 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,751.6 ± 64.7 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.4 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.3 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 38.2 ± 0.2 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 25.6 ± 0.1 | — | 240.0 | — |
| `kitFusedModify` | `-` | 22.8 ± 0.2 | — | 112.0 | — |
| `kitFusedRead` | `-` | 4.5 ± 0.0 | — | 16.0 | — |
| `kitGenericModify` | `-` | 26.9 ± 0.1 | — | 112.0 | — |
| `kitGenericRead` | `-` | 4.5 ± 0.1 | — | 16.0 | — |
| `kitHandModify` | `-` | 18.2 ± 0.6 | — | 112.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.6 ± 0.5 | — | 72.0 | — |
| `reuseLens6` | `-` | 125.2 ± 1.4 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.0 ± 0.2 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,236.3 ± 11.7 | 1,230.9 ± 12.3 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.8 ± 0.7 | 156.2 ± 0.6 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.5 ± 0.1 | 21.0 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,157.1 ± 15.3 | 2,100.3 ± 38.2 | 12,312.4 | 12,312.3 |
| `FoldPrices` | `size=64` | 154.7 ± 0.8 | 155.0 ± 1.2 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.3 ± 0.1 | 26.2 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.4 ± 0.2 | 8.6 ± 0.6 | 0.0 | 0.0 |
| `Get_6` | `-` | 30.1 ± 2.2 | 27.1 ± 1.8 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 409,792.9 ± 5,868.4 | — | 1,066,773.7 | — |
| `cModifyId` | `size=64` | 51,800.4 ± 963.1 | — | 136,293.5 | — |
| `cModifyId` | `size=8` | 8,693.2 ± 134.1 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 203,693.4 ± 4,910.4 | — | 797,929.9 | — |
| `cReadId` | `size=64` | 25,799.3 ± 314.5 | — | 101,294.2 | — |
| `cReadId` | `size=8` | 4,055.0 ± 112.0 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 206,195.8 ± 3,305.4 | — | 797,930.6 | — |
| `cReadStreet` | `size=64` | 26,199.9 ± 687.2 | — | 101,295.1 | — |
| `cReadStreet` | `size=8` | 4,077.7 ± 70.1 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 410,648.7 ± 5,161.7 | — | 1,066,704.9 | — |
| `cReplaceId` | `size=64` | 51,493.2 ± 823.5 | — | 136,245.4 | — |
| `cReplaceId` | `size=8` | 8,662.9 ± 180.6 | — | 20,904.1 | — |
| `cSumPrices` | `size=512` | 346,151.3 ± 3,911.7 | — | 1,240,738.5 | — |
| `cSumPrices` | `size=64` | 41,635.4 ± 376.9 | — | 157,029.0 | — |
| `cSumPrices` | `size=8` | 6,238.0 ± 93.3 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 177.4 ± 1.3 | — | 0.1 | — |
| `jMiss` | `size=64` | 182.1 ± 4.5 | — | 0.0 | — |
| `jMiss` | `size=8` | 177.9 ± 1.1 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,771.7 ± 33.1 | — | 41,929.1 | — |
| `jModifyId` | `size=64` | 474.7 ± 7.5 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 111.9 ± 1.4 | — | 984.0 | — |
| `jReadId` | `size=512` | 34.4 ± 0.6 | — | 48.0 | — |
| `jReadId` | `size=64` | 36.9 ± 6.7 | — | 48.0 | — |
| `jReadId` | `size=8` | 34.7 ± 0.8 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 195.7 ± 3.2 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 194.7 ± 4.1 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 195.2 ± 4.7 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 3,675.7 ± 29.8 | — | 41,905.0 | — |
| `jReplaceId` | `size=64` | 469.8 ± 4.0 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 109.5 ± 2.0 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 88,308.0 ± 903.8 | — | 63,660.5 | — |
| `jSumPrices` | `size=64` | 11,509.4 ± 552.4 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,507.4 ± 20.0 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 88.0 ± 3.4 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.1 ± 0.5 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 64.1 ± 1.3 | — | 216.0 | — |
| `handEnvUse` | `-` | 85.1 ± 1.0 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 33.7 ± 0.3 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 62.2 ± 0.8 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 32.9 ± 0.2 | 29.5 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,113.8 ± 706.5 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,186.8 ± 37.3 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 376.1 ± 2.5 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 85.5 ± 1.0 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 51,884.9 ± 2,310.0 | — | 331,136.8 | — |
| `Modify_multiFocus` | `size=256` | 11,549.8 ± 612.2 | — | 76,134.1 | — |
| `Modify_multiFocus` | `size=32` | 1,356.8 ± 9.7 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 219.5 ± 5.2 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 34,763.2 ± 521.7 | — | 115,177.0 | — |
| `Modify_powerEach` | `size=256` | 8,243.3 ± 67.8 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,014.1 ± 14.0 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 194.2 ± 3.2 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,318.7 ± 74.9 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,058.2 ± 26.4 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 248.6 ± 1.3 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.9 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,079.6 ± 54.3 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 719.4 ± 50.7 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.3 ± 0.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.3 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.7 ± 0.5 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 121.6 ± 1.0 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.6 ± 0.0 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.4 ± 0.3 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.7 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.6 ± 0.3 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.5 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.7 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 156.0 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.5 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,117.2 ± 14.3 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,062.1 ± 12.3 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.1 ± 0.1 | 21.5 ± 0.2 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 44.9 ± 0.2 | 70.9 ± 0.9 | 136.0 | 304.0 |
| `Modify_6` | `-` | 111.6 ± 1.3 | 120.9 ± 1.1 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 40.8 ± 0.5 | 13.5 ± 0.1 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 15.2 ± 0.4 | 16.3 ± 0.1 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.5 ± 0.2 | 7.1 ± 0.1 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 7.7 ± 0.1 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,093.2 ± 1,109.5 | — | 97,423.5 | — |
| `ModifyNames` | `size=64` | 3,981.0 ± 45.6 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 570.7 ± 38.0 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 112.1 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 112.0 ± 2.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 110.6 ± 0.7 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.1 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 36.1 ± 1.4 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 91,871.0 ± 1,576.7 | — | 382,775.4 | — |
| `monocleModifyNames` | `size=64` | 8,918.9 ± 86.3 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,400.8 ± 18.6 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 44,654.4 ± 1,633.9 | — | 169,078.8 | — |
| `monocleModifyStreet` | `size=64` | 5,854.0 ± 189.7 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 928.6 ± 7.5 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 24,926.5 ± 125.1 | — | 69,783.4 | — |
| `monocleReadStreet` | `size=64` | 3,240.0 ± 41.3 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 485.7 ± 13.8 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 58,540.2 ± 1,369.8 | — | 226,295.6 | — |
| `naiveModifyNames` | `size=64` | 7,586.0 ± 33.5 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,092.4 ± 10.0 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,921.1 ± 609.4 | — | 169,056.8 | — |
| `naiveModifyStreet` | `size=64` | 5,976.8 ± 79.0 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 939.3 ± 15.2 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 24,882.6 ± 201.2 | — | 69,783.2 | — |
| `naiveReadStreet` | `size=64` | 3,215.7 ± 21.4 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 476.9 ± 5.8 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 247,883.2 ± 3,558.5 | — | 626,350.7 | — |
| `Names` | `size=64` | 31,311.7 ± 454.4 | — | 78,779.3 | — |
| `Names` | `size=8` | 4,222.8 ± 104.8 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 265,964.5 ± 4,994.9 | — | 691,759.8 | — |
| `NamesIor` | `size=64` | 33,576.0 ± 787.8 | — | 86,445.3 | — |
| `NamesIor` | `size=8` | 4,515.6 ± 76.6 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,068.5 ± 25.6 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,072.6 ± 33.0 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,075.5 ± 23.6 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,063.5 ± 18.5 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,027.9 ± 9.7 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,051.9 ± 17.6 | — | 2,736.0 | — |
| `directNames` | `size=512` | 244,950.2 ± 2,403.4 | — | 613,996.3 | — |
| `directNames` | `size=64` | 29,930.9 ± 683.5 | — | 77,706.7 | — |
| `directNames` | `size=8` | 4,033.3 ± 87.2 | — | 10,544.1 | — |
| `directStreet` | `size=512` | 1,038.6 ± 17.9 | — | 2,736.8 | — |
| `directStreet` | `size=64` | 1,028.6 ± 23.9 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,034.0 ± 11.7 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 246,952.8 ± 5,442.2 | — | 613,997.9 | — |
| `hcursorNames` | `size=64` | 29,987.3 ± 570.6 | — | 77,825.5 | — |
| `hcursorNames` | `size=8` | 4,061.0 ± 80.1 | — | 10,664.1 | — |
| `hcursorStreet` | `size=512` | 1,128.5 ± 33.8 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,145.0 ± 14.6 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,110.4 ± 13.1 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 223,110.6 ± 1,347.4 | — | 1,121,789.9 | — |
| `monocleNames` | `size=64` | 24,252.3 ± 227.9 | — | 132,759.8 | — |
| `monocleNames` | `size=8` | 3,650.8 ± 29.5 | — | 19,498.7 | — |
| `monocleStreet` | `size=512` | 176,875.9 ± 1,161.6 | — | 902,593.5 | — |
| `monocleStreet` | `size=64` | 21,066.0 ± 163.4 | — | 113,831.2 | — |
| `monocleStreet` | `size=8` | 3,195.5 ± 39.0 | — | 17,085.4 | — |
| `naiveNames` | `size=512` | 192,179.1 ± 4,704.9 | — | 965,289.1 | — |
| `naiveNames` | `size=64` | 22,493.3 ± 288.3 | — | 120,493.0 | — |
| `naiveNames` | `size=8` | 3,350.9 ± 28.6 | — | 17,840.1 | — |
| `naiveStreet` | `size=512` | 179,315.0 ± 2,024.4 | — | 908,067.2 | — |
| `naiveStreet` | `size=64` | 21,142.5 ± 295.1 | — | 113,807.2 | — |
| `naiveStreet` | `size=8` | 3,203.4 ± 38.0 | — | 17,077.4 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,889.9 ± 14.5 | — | 42,027.4 | — |
| `ModifyStreet` | `size=64` | 637.7 ± 8.1 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 310.7 ± 20.9 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 195.5 ± 3.2 | — | 136.2 | — |
| `ReadStreet` | `size=64` | 194.5 ± 3.7 | — | 136.0 | — |
| `ReadStreet` | `size=8` | 193.2 ± 2.3 | — | 128.0 | — |
| `SumPrices` | `size=512` | 87,312.0 ± 1,519.5 | — | 63,714.1 | — |
| `SumPrices` | `size=64` | 11,381.0 ± 494.8 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,519.9 ± 12.7 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 163,532.8 ± 1,799.2 | — | 333,578.0 | — |
| `monocleModifyStreet` | `size=64` | 19,772.4 ± 101.6 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,030.5 ± 22.2 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 94,731.8 ± 657.9 | — | 193,233.1 | — |
| `monocleReadStreet` | `size=64` | 11,919.3 ± 127.9 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,793.0 ± 34.5 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 424,789.1 ± 3,417.8 | — | 1,178,445.5 | — |
| `monocleSumPrices` | `size=64` | 15,844.1 ± 160.0 | — | 45,833.6 | — |
| `monocleSumPrices` | `size=8` | 2,345.8 ± 87.7 | — | 6,333.4 | — |
| `naiveModifyStreet` | `size=512` | 163,051.1 ± 1,007.4 | — | 333,569.6 | — |
| `naiveModifyStreet` | `size=64` | 19,744.8 ± 134.4 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,043.3 ± 16.5 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 96,530.5 ± 2,699.5 | — | 193,234.6 | — |
| `naiveReadStreet` | `size=64` | 12,015.7 ± 154.5 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,769.6 ± 14.2 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 100,028.8 ± 685.6 | — | 230,125.6 | — |
| `naiveSumPrices` | `size=64` | 12,827.5 ± 568.9 | — | 29,337.3 | — |
| `naiveSumPrices` | `size=8` | 1,871.1 ± 18.3 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 36,609.2 ± 306.5 | — | 792.6 | — |
| `nativeReadStreet` | `size=64` | 5,192.9 ± 158.3 | — | 760.6 | — |
| `nativeReadStreet` | `size=8` | 975.4 ± 10.4 | — | 760.0 | — |
| `nativeSumPrices` | `size=512` | 74,754.3 ± 1,059.1 | — | 147,839.7 | — |
| `nativeSumPrices` | `size=64` | 9,323.5 ± 87.8 | — | 18,721.1 | — |
| `nativeSumPrices` | `size=8` | 1,388.8 ± 20.5 | — | 2,592.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EverywhereVarName` | `n=4096` | 113,741.8 ± 1,027.2 | — | 491,613.7 | — |
| `EverywhereVarName` | `n=512` | 15,446.9 ± 218.8 | — | 61,449.6 | — |
| `EverywhereVarName` | `n=64` | 1,745.3 ± 16.8 | — | 7,688.0 | — |
| `EverywhereVarNameGeneric` | `n=4096` | 142,265.8 ± 1,109.8 | — | 568,103.5 | — |
| `EverywhereVarNameGeneric` | `n=512` | 21,097.1 ± 273.6 | — | 76,482.2 | — |
| `EverywhereVarNameGeneric` | `n=64` | 2,204.8 ± 31.5 | — | 8,896.0 | — |
| `TransformDeep` | `n=4096` | 130,851.5 ± 2,364.4 | — | 454,999.2 | — |
| `TransformDeep` | `n=512` | 7,421.8 ± 95.6 | — | 20,520.8 | — |
| `TransformDeep` | `n=64` | 739.6 ± 26.9 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 88,530.1 ± 1,150.7 | 162,618.1 ± 1,019.1 | 360,512.4 | 753,734.4 |
| `TransformExpr` | `n=512` | 11,636.9 ± 73.0 | 15,417.1 ± 316.8 | 45,057.2 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,363.4 ± 11.4 | 2,478.1 ± 11.8 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 168,058.7 ± 6,468.8 | — | 766,394.3 | — |
| `TransformMDeep` | `n=512` | 18,758.7 ± 240.7 | — | 82,145.9 | — |
| `TransformMDeep` | `n=64` | 1,701.7 ± 131.2 | — | 6,469.4 | — |
| `TransformMExpr` | `n=4096` | 172,000.8 ± 3,866.9 | 204,340.7 ± 1,760.6 | 566,485.2 | 837,988.7 |
| `TransformMExpr` | `n=512` | 21,953.2 ± 1,156.3 | 23,985.9 ± 1,521.4 | 79,319.6 | 104,338.5 |
| `TransformMExpr` | `n=64` | 2,699.0 ± 86.4 | 3,126.0 ± 34.9 | 8,792.1 | 13,008.1 |
| `UniverseDeep` | `n=4096` | 88,633.1 ± 735.7 | — | 360,688.5 | — |
| `UniverseDeep` | `n=512` | 10,820.1 ± 103.2 | — | 45,185.1 | — |
| `UniverseDeep` | `n=64` | 1,754.0 ± 15.9 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 80,072.0 ± 1,467.5 | 2,847,201.0 ± 192,169.7 | 295,034.3 | 4,687,750.4 |
| `UniverseExpr` | `n=512` | 9,890.1 ± 143.5 | 208,543.1 ± 7,906.5 | 36,929.0 | 475,013.3 |
| `UniverseExpr` | `n=64` | 1,438.7 ± 8.3 | 17,933.6 ± 583.9 | 4,640.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 197,621.0 ± 1,880.3 | 3,156,570.2 ± 155,123.0 | 393,399.8 | 6,489,839.2 |
| `UniverseJson` | `n=512` | 22,966.2 ± 183.0 | 242,948.4 ± 3,865.2 | 49,194.3 | 699,920.8 |
| `UniverseJson` | `n=64` | 2,922.4 ± 27.8 | 22,636.4 ± 775.7 | 6,152.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,092.5 ± 312.5 | — | 163,886.6 | — |
| `visitorTransformDeep` | `n=512` | 4,915.0 ± 42.5 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 424.1 ± 2.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,082.7 ± 489.6 | — | 360,470.6 | — |
| `visitorTransformExpr` | `n=512` | 7,523.1 ± 42.5 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 980.4 ± 8.5 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 66,256.4 ± 10,964.9 | — | 338,693.6 | — |
| `visitorTransformMDeep` | `n=512` | 6,259.6 ± 49.6 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 581.1 ± 11.0 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 86,273.0 ± 962.4 | — | 447,862.8 | — |
| `visitorTransformMExpr` | `n=512` | 10,245.9 ± 214.7 | — | 50,489.0 | — |
| `visitorTransformMExpr` | `n=64` | 1,388.8 ± 88.6 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,028.9 ± 526.7 | — | 196,703.3 | — |
| `visitorUniverseDeep` | `n=512` | 6,500.6 ± 69.5 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 743.7 ± 7.1 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,740.8 ± 399.0 | — | 196,654.4 | — |
| `visitorUniverseExpr` | `n=512` | 6,586.5 ± 122.7 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 803.6 ± 10.1 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 103,343.6 ± 1,403.9 | — | 196,707.2 | — |
| `visitorUniverseJson` | `n=512` | 9,231.2 ± 75.5 | — | 24,600.9 | — |
| `visitorUniverseJson` | `n=64` | 1,352.3 ± 9.0 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,242.7 ± 133.1 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 257.5 ± 1.5 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,971.5 ± 42.9 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 122.8 ± 0.7 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,217.5 ± 1,697.0 | — | 164,371.4 | — |
| `Modify_powerEach` | `size=64` | 794.0 ± 7.3 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 65,701.1 ± 7,083.8 | — | 279,435.7 | — |
| `monocle_powerEach` | `size=16` | 623.0 ± 19.8 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 22,112.7 ± 339.8 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 208.9 ± 1.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 175,714.1 ± 811.1 | — | 967,838.1 | — |
| `monocle_powerEach` | `size=64` | 2,144.2 ± 19.9 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,738.0 ± 73.0 | — | 28,731.1 | — |
| `naive_powerEach` | `size=16` | 101.7 ± 1.1 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,620.0 ± 8.5 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.0 ± 0.2 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,907.9 ± 177.1 | — | 114,787.8 | — |
| `naive_powerEach` | `size=64` | 397.9 ± 2.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 74,147.6 ± 1,158.7 | — | 210,503.0 | — |
| `Modify_nested` | `size=16` | 1,434.0 ± 17.6 | — | 4,720.1 | — |
| `Modify_nested` | `size=256` | 17,543.1 ± 134.8 | — | 53,739.0 | — |
| `Modify_nested` | `size=4` | 681.4 ± 20.3 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,595.7 ± 41.9 | — | 14,616.5 | — |
| `monocle_nested` | `size=1024` | 249,607.2 ± 3,453.5 | — | 1,118,854.4 | — |
| `monocle_nested` | `size=16` | 2,612.0 ± 14.4 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 92,876.4 ± 1,234.9 | — | 430,208.9 | — |
| `monocle_nested` | `size=4` | 1,261.0 ± 108.2 | — | 5,546.7 | — |
| `monocle_nested` | `size=64` | 8,904.6 ± 105.5 | — | 58,833.0 | — |
| `naive_nested` | `size=1024` | 21,719.4 ± 161.0 | — | 115,071.7 | — |
| `naive_nested` | `size=16` | 362.8 ± 3.4 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,231.4 ± 26.7 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 126.0 ± 3.3 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,331.7 ± 14.5 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,315.0 ± 9.1 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,386.4 ± 903.3 | — | 104,683.5 | — |
| `Modify_sparse` | `size=32` | 350.8 ± 9.4 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,661.0 ± 36.8 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 119.4 ± 0.8 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,867.1 ± 39.6 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 105,614.5 ± 779.2 | — | 523,145.7 | — |
| `monocle_sparse` | `size=32` | 1,070.4 ± 36.8 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,708.6 ± 1,069.1 | — | 166,686.2 | — |
| `monocle_sparse` | `size=8` | 348.4 ± 13.4 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 286.6 ± 1.7 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,943.0 ± 53.4 | — | 24,612.2 | — |
| `naive_sparse` | `size=32` | 74.0 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,161.1 ± 16.4 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.0 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.4 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.0 ± 0.3 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.5 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.9 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 95,234.3 ± 907.2 | — | 589,712.7 | — |
| `Cata` | `-` | 60,038.1 ± 428.4 | — | 99,288.4 | — |
| `Hylo` | `-` | 120,075.7 ± 46,673.1 | — | 295,848.8 | — |
| `drosteAna` | `-` | 46,127.5 ± 418.8 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,290.8 ± 359.0 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 64,621.8 ± 352.0 | — | 328,640.5 | — |
| `handAna` | `-` | 20,426.9 ± 135.9 | — | 163,816.1 | — |
| `handCata` | `-` | 12,679.9 ± 61.9 | — | 0.1 | — |
| `handHylo` | `-` | 9,598.4 ± 98.4 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.2 ± 0.1 | 27.9 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.4 ± 0.1 | 63.5 ± 0.5 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.2 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,017.7 ± 83.2 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 532.4 ± 7.8 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 48.9 ± 0.5 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,896.7 ± 52.7 | 31,310.6 ± 341.5 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 154.1 ± 0.9 | 2,070.4 ± 21.1 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.4 ± 0.2 | 324.8 ± 1.7 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,292.6 ± 202.5 | 33,447.0 ± 441.3 | 36,897.2 | 176,890.9 |
| `Modify` | `size=64` | 838.3 ± 6.7 | 1,787.4 ± 32.3 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 94.6 ± 0.6 | 284.4 ± 4.3 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.5 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 91.5 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.2 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 62.9 ± 0.3 | — | 400.0 | — |
| `handDrillGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 87.7 ± 0.6 | — | 592.0 | — |
| `handServiceGet` | `-` | 13.7 ± 0.2 | — | 120.0 | — |
| `handServiceReplace` | `-` | 74.7 ± 0.7 | — | 520.0 | — |


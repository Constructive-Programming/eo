# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `683c0a3b866659bd6902e936cd273388c2213e7d` · date: `2026-11-03` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.6 ± 0.5 | 9.8 ± 0.2 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.6 ± 2.9 | 24.1 ± 1.4 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 159.3 ± 3.5 | — | 720.0 | — |
| `ModifyCountry` | `-` | 308.0 ± 12.0 | — | 928.0 | — |
| `ModifyPartner` | `-` | 403.2 ± 10.0 | — | 984.0 | — |
| `ReadCountry` | `-` | 177.3 ± 3.2 | — | 520.0 | — |
| `ReadPartner` | `-` | 213.7 ± 5.1 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 311.4 ± 5.4 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,650.5 ± 16.1 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,931.3 ± 22.3 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,898.1 ± 61.1 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,689.3 ± 18.4 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,059.0 ± 46.6 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 930.6 ± 6.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 622.2 ± 7.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 401.6 ± 3.5 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 435.0 ± 0.9 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,360.3 ± 46.4 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,348.2 ± 13.3 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 54.0 ± 0.3 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 621.8 ± 6.7 | — | 568.0 | — |
| `handwritten_stream` | `-` | 171.2 ± 20.5 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 658.5 ± 11.0 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,915.7 ± 68.8 | — | 4,541.4 | — |
| `ClickToJson` | `-` | 2,802.5 ± 23.1 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 864.8 ± 3.8 | — | 2,024.0 | — |
| `WideToJson` | `-` | 674.1 ± 18.1 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,449.0 ± 15.7 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,941.5 ± 48.8 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 973.4 ± 11.3 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,879.9 ± 16.1 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 370.2 ± 10.6 | — | 1,202.7 | — |
| `circeDefaultsSparse` | `-` | 326.3 ± 6.3 | — | 754.7 | — |
| `circeStrictFull` | `-` | 368.7 ± 16.4 | — | 1,232.0 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 719.2 ± 9.4 | — | 2,424.1 | — |
| `jsoniterDefaultsFull` | `-` | 1,247.8 ± 4.0 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 333.7 ± 10.4 | — | 640.0 | — |
| `jsoniterStrictFull` | `-` | 1,237.4 ± 4.3 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 497.4 ± 9.0 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 729.6 ± 7.3 | — | 952.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 190.0 ± 0.6 | — | 832.0 | — |
| `decode_native` | `-` | 17.4 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 186.8 ± 0.8 | — | 832.0 | — |
| `encode_bridged` | `-` | 256.1 ± 7.1 | — | 1,256.0 | — |
| `encode_native` | `-` | 11.7 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 254.1 ± 7.2 | — | 1,245.3 | — |
| `fieldGet_bridged` | `-` | 100.0 ± 0.9 | — | 432.0 | — |
| `fieldGet_native` | `-` | 101.2 ± 2.2 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 397.6 ± 8.6 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 171.8 ± 2.3 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.0 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 20.9 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.7 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.4 ± 0.7 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.1 ± 0.1 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 24.2 ± 0.1 | — | 160.0 | — |
| `GetterRead` | `-` | 13.4 ± 0.2 | — | 48.0 | — |
| `Patch` | `-` | 6.4 ± 0.0 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.1 ± 0.1 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 20.9 ± 0.0 | — | 160.0 | — |
| `TransformFocus` | `-` | 829.3 ± 11.7 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,136.4 ± 2.2 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 901.8 ± 6.4 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.5 ± 0.3 | — | 48.0 | — |
| `TransformImage` | `-` | 22.6 ± 0.1 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 22.8 ± 0.1 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.3 ± 0.1 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.4 ± 0.0 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.6 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.5 ± 0.1 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.9 ± 0.0 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.3 ± 0.1 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 808.6 ± 6.6 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 956.1 ± 3.2 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,347.9 ± 20.7 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.6 ± 0.1 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.4 ± 0.0 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.8 ± 0.1 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.3 ± 0.0 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 413.0 ± 1.7 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 392.4 ± 3.7 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 269.4 ± 2.7 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.5 ± 0.0 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.7 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 534.5 ± 12.4 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 530.9 ± 9.3 | — | 744.0 | — |
| `encode_hand` | `-` | 140.5 ± 0.5 | — | 768.0 | — |
| `encode_positional` | `-` | 6,317.9 ± 57.0 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,730.8 ± 40.6 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 38.9 ± 0.9 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 25.8 ± 0.1 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.4 ± 0.0 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.4 ± 0.2 | — | 72.0 | — |
| `reuseLens6` | `-` | 124.6 ± 1.1 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.4 ± 0.3 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,243.4 ± 2.4 | 1,237.1 ± 5.0 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.9 ± 0.4 | 156.8 ± 0.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.4 ± 0.0 | 20.9 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,169.9 ± 22.3 | 2,153.6 ± 31.5 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 156.2 ± 0.9 | 156.6 ± 0.4 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.7 ± 0.1 | 26.3 ± 0.2 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.1 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 2.9 ± 0.1 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.1 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 15.9 ± 0.5 | 8.7 ± 0.8 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.6 ± 0.2 | 25.6 ± 1.9 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.1 ± 0.1 | 3.2 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 422,366.4 ± 23,853.4 | — | 1,066,776.4 | — |
| `cModifyId` | `size=64` | 51,354.6 ± 631.1 | — | 136,293.6 | — |
| `cModifyId` | `size=8` | 8,563.8 ± 102.3 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 205,806.5 ± 2,407.0 | — | 797,930.5 | — |
| `cReadId` | `size=64` | 26,206.8 ± 629.7 | — | 101,294.4 | — |
| `cReadId` | `size=8` | 4,125.2 ± 16.3 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 208,311.2 ± 7,059.6 | — | 797,931.2 | — |
| `cReadStreet` | `size=64` | 26,296.0 ± 315.2 | — | 101,294.4 | — |
| `cReadStreet` | `size=8` | 4,052.9 ± 49.9 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 409,373.7 ± 9,614.5 | — | 1,066,706.4 | — |
| `cReplaceId` | `size=64` | 51,516.4 ± 358.2 | — | 136,245.4 | — |
| `cReplaceId` | `size=8` | 8,506.2 ± 46.1 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 344,509.9 ± 2,682.1 | — | 1,240,742.0 | — |
| `cSumPrices` | `size=64` | 41,463.6 ± 156.8 | — | 157,083.0 | — |
| `cSumPrices` | `size=8` | 6,211.9 ± 86.0 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 180.9 ± 3.5 | — | 0.1 | — |
| `jMiss` | `size=64` | 178.5 ± 4.4 | — | 0.0 | — |
| `jMiss` | `size=8` | 178.7 ± 2.1 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,913.4 ± 25.8 | — | 41,929.1 | — |
| `jModifyId` | `size=64` | 495.4 ± 10.8 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 111.8 ± 0.5 | — | 968.0 | — |
| `jReadId` | `size=512` | 34.1 ± 0.9 | — | 48.0 | — |
| `jReadId` | `size=64` | 34.5 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=8` | 34.7 ± 1.6 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 196.6 ± 3.3 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 192.4 ± 3.5 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 193.9 ± 2.5 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,860.2 ± 26.3 | — | 41,897.1 | — |
| `jReplaceId` | `size=64` | 492.4 ± 2.5 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 112.1 ± 4.2 | — | 936.0 | — |
| `jSumPrices` | `size=512` | 87,594.5 ± 3,262.4 | — | 63,661.4 | — |
| `jSumPrices` | `size=64` | 11,367.4 ± 281.3 | — | 8,120.2 | — |
| `jSumPrices` | `size=8` | 1,528.3 ± 33.7 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 86.9 ± 2.0 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.0 ± 0.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 65.5 ± 1.6 | — | 216.0 | — |
| `handEnvUse` | `-` | 85.5 ± 1.3 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.6 ± 0.8 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 62.7 ± 0.9 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.6 ± 0.8 | 29.7 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.4 ± 0.1 | 3.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,352.6 ± 539.5 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,178.0 ± 55.9 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 376.1 ± 2.3 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 84.9 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 51,852.6 ± 2,560.9 | — | 331,135.8 | — |
| `Modify_multiFocus` | `size=256` | 11,997.9 ± 572.3 | — | 76,155.5 | — |
| `Modify_multiFocus` | `size=32` | 1,370.1 ± 16.8 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 227.9 ± 12.2 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,136.7 ± 833.2 | — | 115,177.1 | — |
| `Modify_powerEach` | `size=256` | 8,287.6 ± 115.6 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,004.3 ± 4.9 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 193.5 ± 3.4 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,338.4 ± 42.4 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,057.9 ± 21.0 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 250.2 ± 0.5 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.0 ± 0.3 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,101.8 ± 54.6 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 719.6 ± 37.8 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.2 ± 0.2 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.0 ± 0.8 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.4 ± 0.6 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 123.2 ± 1.2 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.8 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.7 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.6 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.8 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.5 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 157.0 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.4 ± 0.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,135.2 ± 40.9 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,083.4 ± 41.4 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.1 ± 0.1 | 21.6 ± 0.0 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 44.9 ± 0.1 | 71.1 ± 0.3 | 136.0 | 304.0 |
| `Modify_6` | `-` | 110.3 ± 0.4 | 121.3 ± 1.4 | 184.0 | 496.0 |
| `Modify_loyalty` | `-` | 15.1 ± 0.0 | 16.5 ± 0.5 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.6 ± 0.1 | 7.2 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,070.5 ± 455.4 | — | 97,423.5 | — |
| `ModifyNames` | `size=64` | 4,003.3 ± 74.6 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 543.0 ± 11.0 | — | 1,984.0 | — |
| `ModifyStreet` | `size=512` | 111.0 ± 0.9 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 110.4 ± 0.3 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 111.9 ± 1.6 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.5 ± 1.1 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.3 ± 0.6 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.1 ± 0.3 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 92,387.3 ± 1,052.6 | — | 382,774.9 | — |
| `monocleModifyNames` | `size=64` | 8,984.7 ± 137.7 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,394.1 ± 12.4 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 45,986.0 ± 256.9 | — | 169,080.3 | — |
| `monocleModifyStreet` | `size=64` | 6,024.7 ± 41.9 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 927.8 ± 11.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 25,056.9 ± 184.2 | — | 69,783.6 | — |
| `monocleReadStreet` | `size=64` | 3,264.4 ± 47.1 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 496.6 ± 32.5 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,652.6 ± 444.7 | — | 226,296.1 | — |
| `naiveModifyNames` | `size=64` | 7,606.5 ± 75.9 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,110.2 ± 32.2 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 46,036.9 ± 552.7 | — | 169,056.9 | — |
| `naiveModifyStreet` | `size=64` | 6,004.4 ± 26.7 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 927.7 ± 11.6 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,120.2 ± 167.9 | — | 69,783.3 | — |
| `naiveReadStreet` | `size=64` | 3,225.2 ± 11.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 476.8 ± 5.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 242,504.7 ± 4,291.0 | — | 626,346.4 | — |
| `Names` | `size=64` | 31,034.7 ± 953.1 | — | 79,299.3 | — |
| `Names` | `size=8` | 4,242.3 ± 131.8 | — | 10,949.4 | — |
| `NamesIor` | `size=512` | 265,613.6 ± 2,623.7 | — | 687,660.9 | — |
| `NamesIor` | `size=64` | 32,937.3 ± 867.7 | — | 87,495.3 | — |
| `NamesIor` | `size=8` | 4,463.4 ± 60.3 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,074.1 ± 15.8 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,068.5 ± 19.1 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,070.3 ± 18.8 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,096.1 ± 55.3 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,077.3 ± 16.2 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,044.2 ± 9.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 242,397.6 ± 1,563.0 | — | 609,890.3 | — |
| `directNames` | `size=64` | 30,282.4 ± 234.1 | — | 77,707.2 | — |
| `directNames` | `size=8` | 4,030.9 ± 86.7 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,044.9 ± 10.6 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,037.3 ± 19.1 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 1,065.7 ± 27.0 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 247,095.8 ± 2,948.8 | — | 601,686.0 | — |
| `hcursorNames` | `size=64` | 30,983.1 ± 1,466.0 | — | 76,753.9 | — |
| `hcursorNames` | `size=8` | 4,170.1 ± 96.1 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,148.9 ± 24.6 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,136.8 ± 18.4 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,122.6 ± 16.4 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 226,754.9 ± 2,150.4 | — | 1,121,760.4 | — |
| `monocleNames` | `size=64` | 24,067.5 ± 149.8 | — | 132,759.8 | — |
| `monocleNames` | `size=8` | 3,668.8 ± 45.1 | — | 19,466.7 | — |
| `monocleStreet` | `size=512` | 179,785.6 ± 3,542.2 | — | 908,035.5 | — |
| `monocleStreet` | `size=64` | 20,712.2 ± 355.2 | — | 113,116.5 | — |
| `monocleStreet` | `size=8` | 3,176.5 ± 20.8 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 196,257.7 ± 6,316.2 | — | 965,267.9 | — |
| `naiveNames` | `size=64` | 22,742.5 ± 235.4 | — | 120,845.2 | — |
| `naiveNames` | `size=8` | 3,343.5 ± 8.6 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 182,653.5 ± 5,013.2 | — | 908,037.4 | — |
| `naiveStreet` | `size=64` | 21,101.0 ± 183.8 | — | 113,775.2 | — |
| `naiveStreet` | `size=8` | 3,168.9 ± 10.8 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,119.1 ± 26.7 | — | 42,027.7 | — |
| `ModifyStreet` | `size=64` | 676.8 ± 6.7 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 303.3 ± 18.6 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 199.5 ± 2.2 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 196.3 ± 1.9 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 195.3 ± 4.9 | — | 72.0 | — |
| `SumPrices` | `size=512` | 85,344.1 ± 1,784.8 | — | 63,714.1 | — |
| `SumPrices` | `size=64` | 10,829.2 ± 57.2 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,452.8 ± 14.1 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 158,349.6 ± 1,456.8 | — | 333,571.7 | — |
| `monocleModifyStreet` | `size=64` | 18,877.3 ± 139.1 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,184.1 ± 24.5 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 87,842.1 ± 381.1 | — | 193,227.2 | — |
| `monocleReadStreet` | `size=64` | 11,224.9 ± 212.7 | — | 24,705.1 | — |
| `monocleReadStreet` | `size=8` | 1,818.3 ± 19.0 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 416,134.9 ± 1,915.4 | — | 1,178,436.2 | — |
| `monocleSumPrices` | `size=64` | 15,364.3 ± 218.8 | — | 45,841.6 | — |
| `monocleSumPrices` | `size=8` | 2,464.7 ± 19.2 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 159,670.2 ± 2,758.5 | — | 333,496.1 | — |
| `naiveModifyStreet` | `size=64` | 19,080.0 ± 482.9 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,163.5 ± 11.5 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 88,292.3 ± 688.0 | — | 193,227.6 | — |
| `naiveReadStreet` | `size=64` | 11,507.0 ± 33.9 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,772.2 ± 11.0 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 105,550.3 ± 20,456.4 | — | 230,130.4 | — |
| `naiveSumPrices` | `size=64` | 11,841.7 ± 185.9 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,857.8 ± 17.6 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 32,825.8 ± 341.2 | — | 451.5 | — |
| `nativeReadStreet` | `size=64` | 4,250.6 ± 88.6 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 797.1 ± 8.5 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 61,585.2 ± 824.9 | — | 86,271.0 | — |
| `nativeSumPrices` | `size=64` | 7,738.8 ± 158.0 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,141.9 ± 9.2 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 118,959.8 ± 1,222.8 | — | 624,382.6 | — |
| `TransformDeep` | `n=512` | 11,744.0 ± 61.1 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,420.7 ± 2.8 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 128,709.4 ± 933.0 | 163,511.3 ± 703.6 | 655,357.7 | 753,735.0 |
| `TransformExpr` | `n=512` | 16,000.1 ± 194.7 | 15,210.5 ± 78.2 | 81,825.6 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,989.4 ± 3.7 | 2,499.4 ± 14.8 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 105,342.4 ± 5,711.8 | — | 786,580.7 | — |
| `UniverseDeep` | `n=512` | 15,210.8 ± 79.1 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,862.9 ± 4.7 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 99,332.3 ± 2,561.6 | 2,908,497.0 ± 182,249.6 | 786,384.3 | 4,840,694.3 |
| `UniverseExpr` | `n=512` | 14,756.6 ± 74.4 | 200,973.2 ± 6,383.0 | 98,185.5 | 475,012.5 |
| `UniverseExpr` | `n=64` | 1,795.0 ± 6.3 | 17,947.7 ± 549.5 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 218,136.2 ± 2,281.2 | 3,196,173.6 ± 113,036.8 | 786,470.8 | 6,489,867.9 |
| `UniverseJson` | `n=512` | 24,642.7 ± 56.9 | 238,939.3 ± 1,872.5 | 98,186.5 | 699,920.4 |
| `UniverseJson` | `n=64` | 2,983.6 ± 6.7 | 22,326.8 ± 195.5 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 41,997.6 ± 233.1 | — | 163,886.6 | — |
| `visitorTransformDeep` | `n=512` | 4,895.8 ± 14.6 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 427.4 ± 2.2 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,170.0 ± 314.8 | — | 360,470.7 | — |
| `visitorTransformExpr` | `n=512` | 7,556.6 ± 29.1 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 975.4 ± 10.3 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 55,205.1 ± 503.0 | — | 196,704.2 | — |
| `visitorUniverseDeep` | `n=512` | 6,484.7 ± 54.7 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 746.4 ± 1.0 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,821.3 ± 309.5 | — | 196,654.4 | — |
| `visitorUniverseExpr` | `n=512` | 6,520.0 ± 19.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 798.3 ± 1.3 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 103,569.5 ± 1,131.9 | — | 196,707.4 | — |
| `visitorUniverseJson` | `n=512` | 9,215.5 ± 46.4 | — | 24,600.9 | — |
| `visitorUniverseJson` | `n=64` | 1,347.1 ± 6.8 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,212.5 ± 109.9 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 262.3 ± 4.4 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,042.6 ± 18.5 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 123.1 ± 0.3 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,730.3 ± 1,999.4 | — | 164,372.2 | — |
| `Modify_powerEach` | `size=64` | 779.7 ± 11.4 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 60,027.0 ± 1,336.7 | — | 279,432.8 | — |
| `monocle_powerEach` | `size=16` | 615.2 ± 13.2 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,595.3 ± 79.3 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 210.0 ± 1.8 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 180,714.2 ± 4,612.5 | — | 967,846.3 | — |
| `monocle_powerEach` | `size=64` | 2,169.9 ± 15.7 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,725.5 ± 17.7 | — | 28,731.1 | — |
| `naive_powerEach` | `size=16` | 101.1 ± 0.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,615.1 ± 9.0 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.1 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,960.1 ± 139.2 | — | 114,787.9 | — |
| `naive_powerEach` | `size=64` | 396.5 ± 2.6 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 74,256.9 ± 1,436.5 | — | 210,609.8 | — |
| `Modify_nested` | `size=16` | 1,459.9 ± 7.3 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,521.6 ± 67.7 | — | 53,731.0 | — |
| `Modify_nested` | `size=4` | 688.8 ± 13.7 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,627.1 ± 28.7 | — | 14,608.5 | — |
| `monocle_nested` | `size=1024` | 250,895.7 ± 3,063.0 | — | 1,118,856.8 | — |
| `monocle_nested` | `size=16` | 2,614.5 ± 30.8 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 93,888.0 ± 3,426.7 | — | 430,209.3 | — |
| `monocle_nested` | `size=4` | 1,336.0 ± 101.1 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 8,988.6 ± 86.1 | — | 58,833.8 | — |
| `naive_nested` | `size=1024` | 21,635.6 ± 109.0 | — | 115,071.4 | — |
| `naive_nested` | `size=16` | 365.0 ± 2.3 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,222.3 ± 25.3 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 126.2 ± 2.5 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,320.9 ± 9.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,320.0 ± 8.3 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 24,022.3 ± 703.8 | — | 104,691.4 | — |
| `Modify_sparse` | `size=32` | 348.3 ± 6.1 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,660.9 ± 15.3 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 119.3 ± 0.9 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,878.0 ± 23.5 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 106,246.0 ± 659.9 | — | 523,146.0 | — |
| `monocle_sparse` | `size=32` | 1,062.5 ± 20.7 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,869.8 ± 797.5 | — | 166,686.3 | — |
| `monocle_sparse` | `size=8` | 341.0 ± 4.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 285.9 ± 0.8 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,947.9 ± 16.1 | — | 24,612.0 | — |
| `naive_sparse` | `size=32` | 74.0 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,161.6 ± 3.0 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 24.9 ± 0.0 | — | 128.0 | — |

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
| `ReverseGet_3` | `-` | 19.3 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 33.9 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.7 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.1 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 94,793.3 ± 1,095.7 | — | 589,712.7 | — |
| `Cata` | `-` | 69,923.5 ± 1,157.7 | — | 197,568.5 | — |
| `Hylo` | `-` | 90,134.7 ± 867.1 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,323.5 ± 433.8 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,018.6 ± 112.8 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 62,822.5 ± 742.6 | — | 328,640.4 | — |
| `handAna` | `-` | 20,816.5 ± 202.0 | — | 163,816.1 | — |
| `handCata` | `-` | 12,551.7 ± 43.0 | — | 0.1 | — |
| `handHylo` | `-` | 9,534.5 ± 31.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.3 ± 0.0 | 28.1 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.9 ± 0.4 | 64.2 ± 0.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.1 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,050.4 ± 112.4 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 530.8 ± 2.2 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 49.2 ± 0.2 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,890.5 ± 32.3 | 31,460.4 ± 260.4 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 156.3 ± 0.5 | 2,073.7 ± 14.3 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.6 ± 0.1 | 326.5 ± 1.4 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,318.2 ± 165.6 | 33,346.3 ± 430.9 | 36,897.2 | 176,890.8 |
| `Modify` | `size=64` | 843.3 ± 2.9 | 1,785.5 ± 5.4 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 95.0 ± 0.5 | 284.6 ± 1.8 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.3 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 94.6 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.3 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 66.2 ± 0.5 | — | 400.0 | — |
| `handDrillGet` | `-` | 14.3 ± 0.4 | — | 120.0 | — |
| `handDrillModify` | `-` | 91.4 ± 0.9 | — | 592.0 | — |
| `handServiceGet` | `-` | 14.1 ± 0.2 | — | 120.0 | — |
| `handServiceReplace` | `-` | 74.2 ± 0.6 | — | 520.0 | — |


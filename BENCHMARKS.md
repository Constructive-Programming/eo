# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `720b1afecdb6ddd2342c68af36d652ab6ab367ad` · date: `2026-11-04` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.9 ± 0.1 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 13.5 ± 0.2 | 9.5 ± 0.1 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 25.9 ± 0.3 | 24.0 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 155.9 ± 3.0 | — | 720.0 | — |
| `ModifyCountry` | `-` | 323.8 ± 7.9 | — | 933.3 | — |
| `ModifyPartner` | `-` | 400.5 ± 4.5 | — | 984.0 | — |
| `ReadCountry` | `-` | 185.5 ± 20.8 | — | 520.0 | — |
| `ReadPartner` | `-` | 213.0 ± 3.9 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 314.0 ± 3.2 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,768.5 ± 18.6 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 3,116.6 ± 23.3 | — | 8,696.1 | — |
| `naivePassthroughPayload` | `-` | 5,222.8 ± 82.8 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,742.9 ± 10.9 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,099.1 ± 26.9 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 929.8 ± 7.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 683.4 ± 7.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 377.1 ± 2.5 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 419.0 ± 13.5 | — | 1,546.7 | — |
| `confluentRecordReaderFresh` | `-` | 1,532.8 ± 23.8 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,458.2 ± 20.5 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 58.3 ± 1.0 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 651.2 ± 7.6 | — | 568.0 | — |
| `handwritten_stream` | `-` | 169.5 ± 7.0 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 886.3 ± 250.1 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,019.2 ± 19.9 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,896.7 ± 31.1 | — | 3,952.0 | — |
| `WideToAvro` | `-` | 870.1 ± 15.3 | — | 2,040.0 | — |
| `WideToJson` | `-` | 702.5 ± 20.8 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,562.2 ± 9.1 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 3,138.4 ± 48.2 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 1,084.0 ± 7.0 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,910.3 ± 30.1 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 378.7 ± 3.7 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 336.7 ± 21.2 | — | 720.0 | — |
| `circeStrictFull` | `-` | 381.0 ± 3.6 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 809.4 ± 33.3 | — | 2,698.7 | — |
| `jsoniterDefaultsFull` | `-` | 1,431.7 ± 7.0 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 359.8 ± 5.6 | — | 704.0 | — |
| `jsoniterStrictFull` | `-` | 1,435.4 ± 17.3 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 566.5 ± 8.4 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 738.8 ± 6.0 | — | 1,016.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 209.8 ± 3.7 | — | 832.0 | — |
| `decode_native` | `-` | 17.3 ± 0.3 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 204.9 ± 1.8 | — | 832.0 | — |
| `encode_bridged` | `-` | 256.7 ± 1.5 | — | 1,229.3 | — |
| `encode_native` | `-` | 12.5 ± 0.1 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 262.7 ± 4.2 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 102.0 ± 0.7 | — | 432.0 | — |
| `fieldGet_native` | `-` | 102.6 ± 1.0 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 412.2 ± 6.0 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 176.7 ± 2.0 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 17.7 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 19.0 ± 0.5 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 17.7 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 17.4 ± 0.0 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 5.4 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.9 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 35.1 ± 0.8 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 6.1 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.8 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.2 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.4 ± 0.5 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 29.3 ± 1.0 | — | 160.0 | — |
| `GetterRead` | `-` | 13.4 ± 0.4 | — | 48.0 | — |
| `Patch` | `-` | 7.5 ± 0.2 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 17.8 ± 0.3 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 27.1 ± 0.5 | — | 160.0 | — |
| `TransformFocus` | `-` | 1,043.9 ± 8.7 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,286.7 ± 32.2 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 1,228.8 ± 22.6 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 9.0 ± 0.0 | — | 48.0 | — |
| `TransformImage` | `-` | 22.1 ± 0.4 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 25.2 ± 0.5 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 37.1 ± 0.6 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.9 ± 0.2 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 13.5 ± 0.2 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 6.7 ± 0.1 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.8 ± 0.3 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 26.8 ± 0.5 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 970.9 ± 20.1 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 1,106.4 ± 6.5 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,735.8 ± 17.3 | — | 10,578.7 | — |
| `chimneyTransformFocusWrite` | `-` | 6.6 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.0 ± 0.1 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 16.2 ± 0.5 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 23.7 ± 0.3 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.5 ± 0.2 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 6.9 ± 0.1 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 2.6 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 2.5 ± 0.1 | — | 16.0 | — |
| `handTransformFocus` | `-` | 512.3 ± 8.5 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 650.1 ± 5.2 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 456.5 ± 6.3 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 6.7 ± 0.1 | — | 48.0 | — |
| `handTransformImage` | `-` | 19.1 ± 0.3 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 9.1 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 1.2 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 503.1 ± 10.1 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 498.0 ± 4.9 | — | 744.0 | — |
| `encode_hand` | `-` | 174.0 ± 0.5 | — | 768.0 | — |
| `encode_positional` | `-` | 5,546.3 ± 109.8 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 6,487.9 ± 116.2 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 10.2 ± 0.3 | — | 72.0 | — |
| `buildLens3` | `-` | 29.0 ± 0.6 | — | 184.0 | — |
| `buildLens6` | `-` | 57.7 ± 0.3 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 38.1 ± 0.4 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.7 ± 0.1 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.4 ± 0.6 | — | 40.0 | — |
| `reuseLens3` | `-` | 40.6 ± 1.0 | — | 72.0 | — |
| `reuseLens6` | `-` | 111.3 ± 1.8 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 41.7 ± 0.3 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,385.6 ± 16.8 | 1,373.7 ± 10.6 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 158.1 ± 0.5 | 156.6 ± 0.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 17.3 ± 0.0 | 17.2 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,429.8 ± 23.4 | 2,441.6 ± 67.9 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 216.5 ± 5.5 | 218.9 ± 2.1 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 29.7 ± 0.2 | 30.4 ± 0.4 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 4.1 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.6 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.6 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.3 ± 0.1 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.4 ± 0.1 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 1.0 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 15.7 ± 2.6 | 7.7 ± 0.1 | 0.0 | 0.0 |
| `Get_6` | `-` | 27.5 ± 1.6 | 22.7 ± 1.6 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 1.0 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.7 ± 0.0 | 4.9 ± 0.2 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 4.3 ± 0.1 | 4.4 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 409,043.4 ± 9,475.5 | — | 1,066,755.8 | — |
| `cModifyId` | `size=64` | 52,478.0 ± 480.9 | — | 136,293.6 | — |
| `cModifyId` | `size=8` | 8,525.6 ± 309.3 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 213,896.7 ± 5,830.7 | — | 797,932.8 | — |
| `cReadId` | `size=64` | 26,835.5 ± 191.3 | — | 101,294.2 | — |
| `cReadId` | `size=8` | 4,053.8 ± 28.5 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 210,976.7 ± 2,817.6 | — | 797,932.0 | — |
| `cReadStreet` | `size=64` | 26,617.9 ± 521.5 | — | 101,293.8 | — |
| `cReadStreet` | `size=8` | 4,044.9 ± 90.4 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 415,100.1 ± 8,565.6 | — | 1,066,698.3 | — |
| `cReplaceId` | `size=64` | 52,801.7 ± 913.1 | — | 136,237.7 | — |
| `cReplaceId` | `size=8` | 8,371.9 ± 66.4 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 353,538.8 ± 11,695.0 | — | 1,240,743.8 | — |
| `cSumPrices` | `size=64` | 43,797.0 ± 338.1 | — | 157,021.6 | — |
| `cSumPrices` | `size=8` | 6,355.7 ± 180.2 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 167.4 ± 0.8 | — | 0.0 | — |
| `jMiss` | `size=64` | 167.7 ± 2.2 | — | 0.0 | — |
| `jMiss` | `size=8` | 168.0 ± 2.4 | — | 0.0 | — |
| `jModifyId` | `size=512` | 5,938.1 ± 77.3 | — | 41,921.7 | — |
| `jModifyId` | `size=64` | 725.3 ± 16.5 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 152.3 ± 2.4 | — | 992.0 | — |
| `jReadId` | `size=512` | 35.1 ± 1.6 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.3 ± 1.6 | — | 48.0 | — |
| `jReadId` | `size=8` | 37.5 ± 4.4 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 185.6 ± 3.4 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 186.2 ± 1.7 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 185.1 ± 1.5 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 5,846.3 ± 81.2 | — | 41,897.7 | — |
| `jReplaceId` | `size=64` | 701.4 ± 10.5 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 151.8 ± 2.5 | — | 960.0 | — |
| `jSumPrices` | `size=512` | 83,168.9 ± 768.5 | — | 63,664.0 | — |
| `jSumPrices` | `size=64` | 10,973.4 ± 103.5 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,500.5 ± 45.2 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 105.6 ± 1.1 | — | 320.0 | — |
| `MapDrillModify` | `-` | 41.1 ± 2.3 | — | 216.0 | — |
| `MapGet` | `-` | 2.5 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 76.3 ± 0.9 | — | 216.0 | — |
| `handEnvUse` | `-` | 104.3 ± 1.1 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 36.9 ± 1.0 | — | 216.0 | — |
| `handMapGet` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 73.7 ± 0.9 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.2 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.4 ± 0.1 | 5.8 ± 0.1 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 35.0 ± 0.5 | 30.8 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 5.3 ± 0.3 | 5.0 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 16,513.1 ± 953.9 | — | 43,036.3 | — |
| `Fold_powerEach` | `size=256` | 3,274.0 ± 41.2 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 372.4 ± 3.3 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 77.2 ± 1.1 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 61,206.1 ± 2,184.6 | — | 331,114.0 | — |
| `Modify_multiFocus` | `size=256` | 13,896.7 ± 452.0 | — | 76,129.0 | — |
| `Modify_multiFocus` | `size=32` | 1,658.1 ± 44.6 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 242.2 ± 2.1 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,783.0 ± 679.6 | — | 115,177.3 | — |
| `Modify_powerEach` | `size=256` | 8,151.2 ± 115.5 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,000.6 ± 15.7 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 191.3 ± 6.1 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 10,500.9 ± 180.8 | — | 65,578.7 | — |
| `naive_listMap` | `size=256` | 2,559.8 ± 41.2 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 317.2 ± 6.6 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 44.3 ± 0.5 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,676.3 ± 147.6 | — | 16,129.2 | — |
| `naive_sumQty` | `size=256` | 690.5 ± 4.3 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 67.8 ± 1.5 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 6.9 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 34.3 ± 0.2 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 141.0 ± 0.8 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 13.8 ± 0.2 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 37.3 ± 0.3 | — | 248.0 | — |
| `naive_constSum` | `-` | 2.7 ± 0.1 | — | 16.0 | — |
| `naive_listSum` | `-` | 30.7 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 13.3 ± 0.2 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 24.6 ± 0.5 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 213.1 ± 3.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 45.4 ± 1.0 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,090.3 ± 21.7 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,007.8 ± 12.2 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.1 ± 0.2 | 22.3 ± 0.4 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.2 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 42.0 ± 0.4 | 68.4 ± 0.9 | 136.0 | 304.0 |
| `Modify_6` | `-` | 100.5 ± 2.1 | 115.6 ± 4.6 | 184.0 | 496.0 |
| `Modify_loyalty` | `-` | 15.6 ± 0.1 | 19.5 ± 0.3 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 6.3 ± 0.1 | 6.1 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 11.6 ± 0.2 | 12.0 ± 0.6 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 33,314.5 ± 675.4 | — | 97,423.8 | — |
| `ModifyNames` | `size=64` | 4,041.0 ± 41.2 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 559.8 ± 9.2 | — | 1,976.0 | — |
| `ModifyStreet` | `size=512` | 122.8 ± 1.4 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 122.2 ± 0.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 123.1 ± 1.4 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.7 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.2 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 36.8 ± 2.9 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 94,588.7 ± 489.4 | — | 382,774.9 | — |
| `monocleModifyNames` | `size=64` | 9,757.2 ± 134.3 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,507.9 ± 46.3 | — | 5,405.3 | — |
| `monocleModifyStreet` | `size=512` | 46,965.8 ± 344.8 | — | 169,080.2 | — |
| `monocleModifyStreet` | `size=64` | 6,089.4 ± 24.0 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 946.2 ± 13.6 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 25,213.9 ± 208.8 | — | 69,783.1 | — |
| `monocleReadStreet` | `size=64` | 3,181.8 ± 12.1 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 483.1 ± 4.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,396.2 ± 884.0 | — | 226,296.0 | — |
| `naiveModifyNames` | `size=64` | 7,675.4 ± 114.9 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,159.0 ± 14.6 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 47,119.2 ± 928.5 | — | 169,057.4 | — |
| `naiveModifyStreet` | `size=64` | 6,136.2 ± 82.3 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 941.9 ± 7.4 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,072.3 ± 422.9 | — | 69,782.7 | — |
| `naiveReadStreet` | `size=64` | 3,173.9 ± 15.1 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 480.0 ± 2.9 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 237,275.0 ± 5,288.7 | — | 622,238.2 | — |
| `Names` | `size=64` | 29,891.3 ± 483.9 | — | 79,299.2 | — |
| `Names` | `size=8` | 4,062.4 ± 23.5 | — | 10,872.1 | — |
| `NamesIor` | `size=512` | 261,265.2 ± 5,121.8 | — | 691,761.4 | — |
| `NamesIor` | `size=64` | 32,878.5 ± 556.2 | — | 87,486.8 | — |
| `NamesIor` | `size=8` | 4,303.5 ± 42.0 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,041.7 ± 16.7 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,035.9 ± 11.7 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,038.3 ± 13.8 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,040.0 ± 17.2 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,056.1 ± 44.1 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,031.0 ± 22.8 | — | 2,736.0 | — |
| `directNames` | `size=512` | 237,775.1 ± 5,397.5 | — | 609,886.6 | — |
| `directNames` | `size=64` | 29,207.3 ± 241.9 | — | 77,705.0 | — |
| `directNames` | `size=8` | 4,034.1 ± 51.6 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,033.5 ± 6.8 | — | 2,742.1 | — |
| `directStreet` | `size=64` | 1,025.7 ± 14.7 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,044.4 ± 23.1 | — | 2,728.0 | — |
| `hcursorNames` | `size=512` | 236,044.8 ± 2,499.0 | — | 609,885.2 | — |
| `hcursorNames` | `size=64` | 30,373.6 ± 488.6 | — | 77,790.0 | — |
| `hcursorNames` | `size=8` | 4,090.6 ± 79.3 | — | 10,768.1 | — |
| `hcursorStreet` | `size=512` | 1,114.9 ± 11.7 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,106.7 ± 12.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,130.8 ± 17.2 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 235,494.8 ± 5,763.3 | — | 1,121,766.2 | — |
| `monocleNames` | `size=64` | 26,029.9 ± 294.4 | — | 132,776.7 | — |
| `monocleNames` | `size=8` | 4,022.4 ± 31.9 | — | 19,557.4 | — |
| `monocleStreet` | `size=512` | 179,048.2 ± 894.0 | — | 908,024.3 | — |
| `monocleStreet` | `size=64` | 22,308.2 ± 275.7 | — | 113,799.3 | — |
| `monocleStreet` | `size=8` | 3,435.1 ± 58.6 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 193,661.0 ± 2,685.0 | — | 965,266.1 | — |
| `naiveNames` | `size=64` | 24,056.1 ± 378.7 | — | 120,832.1 | — |
| `naiveNames` | `size=8` | 3,626.0 ± 61.3 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 178,583.9 ± 1,518.7 | — | 908,029.3 | — |
| `naiveStreet` | `size=64` | 22,229.0 ± 279.7 | — | 113,770.0 | — |
| `naiveStreet` | `size=8` | 3,387.6 ± 27.5 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 6,114.1 ± 56.3 | — | 42,029.5 | — |
| `ModifyStreet` | `size=64` | 890.1 ± 7.7 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 330.8 ± 7.0 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 188.7 ± 1.3 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 184.9 ± 3.1 | — | 90.7 | — |
| `ReadStreet` | `size=8` | 182.7 ± 5.5 | — | 109.3 | — |
| `SumPrices` | `size=512` | 84,503.3 ± 2,981.8 | — | 63,715.2 | — |
| `SumPrices` | `size=64` | 10,776.7 ± 149.2 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,495.7 ± 66.0 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 160,463.4 ± 1,855.6 | — | 333,574.1 | — |
| `monocleModifyStreet` | `size=64` | 19,566.5 ± 334.7 | — | 30,082.0 | — |
| `monocleModifyStreet` | `size=8` | 3,389.8 ± 40.7 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 86,075.7 ± 1,303.3 | — | 193,225.7 | — |
| `monocleReadStreet` | `size=64` | 11,030.1 ± 290.4 | — | 24,705.1 | — |
| `monocleReadStreet` | `size=8` | 1,844.9 ± 24.3 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 397,707.3 ± 4,532.1 | — | 1,178,420.4 | — |
| `monocleSumPrices` | `size=64` | 15,852.8 ± 159.4 | — | 45,833.6 | — |
| `monocleSumPrices` | `size=8` | 2,603.0 ± 31.6 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 161,332.8 ± 4,963.2 | — | 333,566.8 | — |
| `naiveModifyStreet` | `size=64` | 19,440.2 ± 389.6 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,350.5 ± 23.8 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 85,210.8 ± 1,223.7 | — | 193,224.9 | — |
| `naiveReadStreet` | `size=64` | 10,964.6 ± 222.3 | — | 24,705.1 | — |
| `naiveReadStreet` | `size=8` | 1,837.1 ± 7.7 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 92,495.0 ± 401.5 | — | 230,119.2 | — |
| `naiveSumPrices` | `size=64` | 11,885.1 ± 236.9 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,951.0 ± 15.0 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,412.4 ± 541.5 | — | 451.7 | — |
| `nativeReadStreet` | `size=64` | 4,295.1 ± 106.1 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 795.1 ± 5.8 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 59,361.4 ± 1,037.2 | — | 86,271.8 | — |
| `nativeSumPrices` | `size=64` | 7,378.8 ± 125.6 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,105.6 ± 5.4 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 145,837.9 ± 2,177.5 | — | 624,402.1 | — |
| `TransformDeep` | `n=512` | 16,978.8 ± 77.6 | — | 57,361.7 | — |
| `TransformDeep` | `n=64` | 1,982.3 ± 29.3 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 138,961.9 ± 941.1 | 175,829.0 ± 3,849.5 | 655,365.1 | 753,744.0 |
| `TransformExpr` | `n=512` | 17,175.5 ± 82.9 | 16,396.4 ± 239.0 | 81,825.8 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,145.0 ± 29.4 | 2,697.3 ± 31.4 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 154,410.8 ± 2,635.5 | — | 786,616.4 | — |
| `UniverseDeep` | `n=512` | 18,631.9 ± 263.9 | — | 98,377.9 | — |
| `UniverseDeep` | `n=64` | 2,242.2 ± 68.1 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 153,152.9 ± 4,082.3 | 4,368,052.5 ± 168,974.4 | 786,423.5 | 4,841,752.4 |
| `UniverseExpr` | `n=512` | 18,066.9 ± 286.1 | 325,250.9 ± 4,463.6 | 98,185.8 | 475,025.2 |
| `UniverseExpr` | `n=64` | 2,159.1 ± 51.8 | 28,961.8 ± 404.4 | 12,168.0 | 45,424.6 |
| `UniverseJson` | `n=4096` | 242,368.6 ± 2,737.8 | 4,702,383.6 ± 172,860.9 | 786,488.4 | 6,490,956.1 |
| `UniverseJson` | `n=512` | 27,160.6 ± 535.5 | 364,494.5 ± 3,754.7 | 98,186.8 | 699,933.4 |
| `UniverseJson` | `n=64` | 3,141.1 ± 57.1 | 33,779.3 ± 658.7 | 12,168.1 | 73,208.7 |
| `visitorTransformDeep` | `n=4096` | 51,608.0 ± 1,434.5 | — | 163,893.6 | — |
| `visitorTransformDeep` | `n=512` | 5,861.6 ± 74.7 | — | 20,496.6 | — |
| `visitorTransformDeep` | `n=64` | 618.9 ± 8.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 72,255.9 ± 1,001.9 | — | 360,476.6 | — |
| `visitorTransformExpr` | `n=512` | 8,833.7 ± 235.4 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,121.9 ± 33.5 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 73,371.6 ± 884.5 | — | 196,717.4 | — |
| `visitorUniverseDeep` | `n=512` | 9,052.4 ± 136.3 | — | 24,632.9 | — |
| `visitorUniverseDeep` | `n=64` | 971.1 ± 21.2 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 54,610.0 ± 369.6 | — | 196,655.8 | — |
| `visitorUniverseExpr` | `n=512` | 6,753.0 ± 100.4 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 793.7 ± 3.9 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 95,988.5 ± 363.1 | — | 196,701.9 | — |
| `visitorUniverseJson` | `n=512` | 10,004.9 ± 72.5 | — | 24,601.0 | — |
| `visitorUniverseJson` | `n=64` | 1,417.6 ± 15.5 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 14,022.2 ± 192.7 | — | 41,414.6 | — |
| `Modify_powerEach` | `size=16` | 262.3 ± 3.7 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,408.6 ± 71.1 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 118.9 ± 1.8 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 57,077.5 ± 698.6 | — | 164,381.0 | — |
| `Modify_powerEach` | `size=64` | 880.2 ± 3.5 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 62,467.1 ± 906.4 | — | 279,434.5 | — |
| `monocle_powerEach` | `size=16` | 729.6 ± 14.0 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 22,500.4 ± 498.9 | — | 107,331.5 | — |
| `monocle_powerEach` | `size=4` | 215.2 ± 12.3 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 215,841.8 ± 1,200.2 | — | 967,903.4 | — |
| `monocle_powerEach` | `size=64` | 2,657.3 ± 13.1 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 5,758.2 ± 85.3 | — | 28,730.7 | — |
| `naive_powerEach` | `size=16` | 92.6 ± 1.4 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,353.1 ± 28.3 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 28.6 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 23,385.2 ± 423.2 | — | 114,782.1 | — |
| `naive_powerEach` | `size=64` | 346.6 ± 6.0 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 78,030.7 ± 758.9 | — | 210,584.7 | — |
| `Modify_nested` | `size=16` | 1,580.4 ± 5.8 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 19,181.2 ± 304.3 | — | 53,740.0 | — |
| `Modify_nested` | `size=4` | 742.6 ± 14.3 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 5,056.0 ± 85.4 | — | 14,664.6 | — |
| `monocle_nested` | `size=1024` | 256,817.0 ± 4,655.3 | — | 1,118,867.5 | — |
| `monocle_nested` | `size=16` | 2,999.3 ± 31.0 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 96,534.8 ± 1,627.8 | — | 430,210.6 | — |
| `monocle_nested` | `size=4` | 1,315.5 ± 51.9 | — | 5,536.0 | — |
| `monocle_nested` | `size=64` | 10,861.4 ± 304.1 | — | 58,833.2 | — |
| `naive_nested` | `size=1024` | 23,312.7 ± 617.4 | — | 115,074.4 | — |
| `naive_nested` | `size=16` | 387.1 ± 8.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,686.4 ± 124.6 | — | 29,019.5 | — |
| `naive_nested` | `size=4` | 140.4 ± 1.8 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,370.5 ± 21.4 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,484.6 ± 49.3 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 26,547.5 ± 987.9 | — | 104,686.3 | — |
| `Modify_sparse` | `size=32` | 379.5 ± 11.3 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 6,314.2 ± 93.2 | — | 24,785.7 | — |
| `Modify_sparse` | `size=8` | 125.7 ± 2.6 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,888.7 ± 128.4 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 117,201.1 ± 4,208.3 | — | 523,152.6 | — |
| `monocle_sparse` | `size=32` | 1,270.7 ± 15.4 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 34,329.9 ± 507.2 | — | 166,686.2 | — |
| `monocle_sparse` | `size=8` | 395.9 ± 14.5 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 351.1 ± 8.0 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,956.4 ± 133.3 | — | 24,612.9 | — |
| `naive_sparse` | `size=32` | 91.0 ± 1.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,426.7 ± 25.2 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 25.5 ± 0.3 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.2 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.7 ± 0.0 | 2.9 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.8 ± 0.1 | 2.8 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.7 ± 0.0 | 2.8 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 18.3 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 32.5 ± 0.9 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.4 ± 0.1 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 11.0 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 19.1 ± 0.3 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 105,417.1 ± 2,128.9 | — | 589,712.7 | — |
| `Cata` | `-` | 67,382.4 ± 533.8 | — | 197,568.5 | — |
| `Hylo` | `-` | 87,666.8 ± 661.9 | — | 295,848.6 | — |
| `drosteAna` | `-` | 55,435.0 ± 2,660.0 | — | 327,632.4 | — |
| `drosteCata` | `-` | 40,930.3 ± 2,406.1 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 59,553.9 ± 235.9 | — | 328,640.4 | — |
| `handAna` | `-` | 29,482.2 ± 945.8 | — | 163,816.2 | — |
| `handCata` | `-` | 13,684.5 ± 63.7 | — | 0.1 | — |
| `handHylo` | `-` | 11,041.6 ± 160.5 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.5 ± 0.0 | 3.5 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 13.3 ± 0.2 | 31.1 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.5 ± 0.6 | 62.0 ± 3.6 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 5.1 ± 0.0 | 5.2 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,527.0 ± 81.8 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 649.4 ± 8.7 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 61.6 ± 6.1 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 2,148.3 ± 71.1 | 32,415.4 ± 796.6 | 12,312.3 | 162,565.1 |
| `FoldPrices` | `size=64` | 219.7 ± 2.0 | 2,715.1 ± 22.0 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 30.9 ± 1.0 | 406.4 ± 8.1 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 8,552.0 ± 260.5 | 35,671.0 ± 1,296.1 | 36,897.4 | 176,891.8 |
| `Modify` | `size=64` | 918.8 ± 10.8 | 2,484.3 ± 18.7 | 4,640.0 | 14,408.1 |
| `Modify` | `size=8` | 107.9 ± 0.6 | 371.5 ± 6.6 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 11.1 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 111.4 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.6 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 87.4 ± 1.9 | — | 400.0 | — |
| `handDrillGet` | `-` | 20.3 ± 0.5 | — | 120.0 | — |
| `handDrillModify` | `-` | 108.2 ± 1.2 | — | 592.0 | — |
| `handServiceGet` | `-` | 20.4 ± 0.6 | — | 120.0 | — |
| `handServiceReplace` | `-` | 94.1 ± 1.5 | — | 520.0 | — |


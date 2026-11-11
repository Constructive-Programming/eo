# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `d4429ef356d74617ab5afa64450903a1dca30763` · date: `2026-11-11` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.6 ± 0.2 | 10.2 ± 0.4 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 28.7 ± 0.2 | 24.5 ± 0.9 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 160.9 ± 3.8 | — | 720.0 | — |
| `ModifyCountry` | `-` | 299.0 ± 12.9 | — | 928.0 | — |
| `ModifyPartner` | `-` | 393.1 ± 16.6 | — | 984.0 | — |
| `ReadCountry` | `-` | 173.8 ± 3.8 | — | 520.0 | — |
| `ReadPartner` | `-` | 209.2 ± 2.7 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 307.6 ± 2.3 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,614.2 ± 19.6 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,890.9 ± 41.6 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,798.2 ± 39.6 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,671.3 ± 23.3 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,003.5 ± 27.2 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 948.5 ± 15.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 610.5 ± 5.8 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 408.8 ± 10.4 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 433.5 ± 9.4 | — | 1,530.7 | — |
| `confluentRecordReaderFresh` | `-` | 1,352.9 ± 36.0 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,339.0 ± 23.2 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 53.6 ± 0.5 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 635.0 ± 23.4 | — | 568.0 | — |
| `handwritten_stream` | `-` | 158.1 ± 2.1 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 644.4 ± 3.9 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,914.3 ± 39.4 | — | 4,674.7 | — |
| `ClickToJson` | `-` | 2,747.5 ± 25.3 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 864.3 ± 16.7 | — | 2,029.3 | — |
| `WideToJson` | `-` | 688.1 ± 14.7 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,450.2 ± 17.2 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,911.6 ± 45.8 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 954.8 ± 14.5 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,896.5 ± 52.2 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 368.1 ± 7.7 | — | 1,216.0 | — |
| `circeDefaultsSparse` | `-` | 320.4 ± 4.7 | — | 800.0 | — |
| `circeStrictFull` | `-` | 370.5 ± 8.7 | — | 1,216.0 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 732.7 ± 21.9 | — | 2,576.1 | — |
| `jsoniterDefaultsFull` | `-` | 1,235.1 ± 20.5 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 339.2 ± 12.3 | — | 672.0 | — |
| `jsoniterStrictFull` | `-` | 1,225.4 ± 13.8 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 497.0 ± 4.5 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 734.2 ± 9.3 | — | 1,048.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 191.9 ± 1.4 | — | 832.0 | — |
| `decode_native` | `-` | 17.5 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 187.5 ± 2.2 | — | 832.0 | — |
| `encode_bridged` | `-` | 258.2 ± 1.9 | — | 1,272.0 | — |
| `encode_native` | `-` | 12.2 ± 0.8 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 260.8 ± 1.9 | — | 1,256.0 | — |
| `fieldGet_bridged` | `-` | 101.2 ± 1.8 | — | 432.0 | — |
| `fieldGet_native` | `-` | 102.6 ± 1.3 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 403.9 ± 4.2 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 171.7 ± 5.1 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.1 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.0 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.4 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.7 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.2 ± 0.4 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.1 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.1 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.3 ± 0.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 24.4 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 13.4 ± 0.5 | — | 48.0 | — |
| `Patch` | `-` | 6.4 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.1 ± 0.1 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 20.9 ± 0.1 | — | 160.0 | — |
| `TransformFocus` | `-` | 833.8 ± 14.9 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,138.3 ± 11.6 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 894.5 ± 7.0 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.3 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 22.6 ± 0.1 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 22.9 ± 0.1 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.3 ± 0.3 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.5 ± 0.3 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.6 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.4 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 20.2 ± 9.7 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.2 ± 0.1 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 846.2 ± 51.6 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 954.2 ± 4.3 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,408.6 ± 121.3 | — | 10,578.7 | — |
| `chimneyTransformFocusWrite` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.5 ± 0.2 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.4 ± 0.2 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.8 ± 0.1 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.2 ± 0.2 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.2 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 414.9 ± 4.0 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 385.1 ± 2.4 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 265.6 ± 1.7 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.7 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.5 ± 0.2 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.6 ± 0.0 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 526.7 ± 8.0 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 524.9 ± 7.8 | — | 744.0 | — |
| `encode_hand` | `-` | 140.3 ± 1.4 | — | 768.0 | — |
| `encode_positional` | `-` | 6,347.9 ± 54.4 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,746.6 ± 63.5 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.5 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.4 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 38.6 ± 0.3 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 25.8 ± 0.1 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.3 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.6 ± 0.8 | — | 72.0 | — |
| `reuseLens6` | `-` | 124.2 ± 0.9 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.1 ± 0.4 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,237.0 ± 8.9 | 1,241.1 ± 10.2 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.7 ± 0.9 | 156.7 ± 1.0 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.4 ± 0.1 | 20.9 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,135.1 ± 46.2 | 2,078.4 ± 28.7 | 12,312.3 | 12,312.3 |
| `FoldPrices` | `size=64` | 155.4 ± 1.1 | 155.7 ± 0.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.5 ± 0.1 | 26.3 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
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
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.2 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.3 ± 0.1 | 9.0 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 29.0 ± 0.3 | 26.3 ± 1.4 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.0 ± 0.0 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 406,622.7 ± 5,314.7 | — | 1,066,754.3 | — |
| `cModifyId` | `size=64` | 51,033.2 ± 496.7 | — | 136,294.0 | — |
| `cModifyId` | `size=8` | 8,445.1 ± 113.4 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 206,148.0 ± 3,877.0 | — | 797,930.6 | — |
| `cReadId` | `size=64` | 26,678.4 ± 783.6 | — | 101,295.0 | — |
| `cReadId` | `size=8` | 4,159.8 ± 73.7 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 204,476.5 ± 993.3 | — | 797,930.2 | — |
| `cReadStreet` | `size=64` | 26,490.4 ± 1,529.1 | — | 101,294.9 | — |
| `cReadStreet` | `size=8` | 4,119.2 ± 62.0 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 408,922.5 ± 11,505.1 | — | 1,066,707.1 | — |
| `cReplaceId` | `size=64` | 52,283.2 ± 979.6 | — | 136,245.9 | — |
| `cReplaceId` | `size=8` | 8,474.6 ± 63.0 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 341,583.4 ± 4,928.5 | — | 1,240,740.3 | — |
| `cSumPrices` | `size=64` | 44,155.7 ± 4,499.0 | — | 156,978.9 | — |
| `cSumPrices` | `size=8` | 6,137.5 ± 72.9 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 176.9 ± 5.9 | — | 0.1 | — |
| `jMiss` | `size=64` | 179.6 ± 5.4 | — | 0.0 | — |
| `jMiss` | `size=8` | 179.9 ± 3.9 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,682.5 ± 19.6 | — | 41,929.0 | — |
| `jModifyId` | `size=64` | 469.7 ± 5.0 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 112.3 ± 2.1 | — | 984.0 | — |
| `jReadId` | `size=512` | 35.0 ± 1.0 | — | 48.0 | — |
| `jReadId` | `size=64` | 34.4 ± 0.5 | — | 48.0 | — |
| `jReadId` | `size=8` | 35.0 ± 1.0 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 193.7 ± 2.4 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 196.5 ± 3.1 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 192.5 ± 1.4 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,626.0 ± 15.4 | — | 41,897.0 | — |
| `jReplaceId` | `size=64` | 447.0 ± 3.7 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 107.1 ± 2.4 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 87,236.9 ± 3,012.2 | — | 63,660.7 | — |
| `jSumPrices` | `size=64` | 11,277.6 ± 402.2 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,530.8 ± 65.0 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 85.9 ± 1.2 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.0 ± 0.3 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 66.1 ± 2.1 | — | 216.0 | — |
| `handEnvUse` | `-` | 84.8 ± 0.6 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.1 ± 0.5 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 63.1 ± 1.8 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.1 ± 0.3 | 30.1 ± 0.8 | 152.0 | 176.0 |
| `Replace` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,285.0 ± 558.8 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,193.0 ± 93.6 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 377.1 ± 6.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 84.7 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 52,790.6 ± 1,193.7 | — | 331,156.2 | — |
| `Modify_multiFocus` | `size=256` | 11,181.6 ± 76.3 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,380.9 ± 19.5 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 218.8 ± 2.3 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 34,712.7 ± 435.6 | — | 115,177.0 | — |
| `Modify_powerEach` | `size=256` | 8,213.8 ± 69.4 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 925.0 ± 126.7 | — | 3,168.0 | — |
| `Modify_powerEach` | `size=4` | 192.4 ± 3.7 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,349.5 ± 68.8 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,051.9 ± 14.9 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 249.9 ± 0.8 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.9 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,104.3 ± 90.5 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 715.0 ± 49.4 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.2 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.5 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.7 ± 0.7 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 122.7 ± 1.0 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.6 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.5 ± 0.2 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.7 ± 0.2 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.4 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 13.8 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 156.2 ± 0.8 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.6 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,127.0 ± 17.9 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,051.3 ± 8.7 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.0 ± 0.1 | 21.6 ± 0.2 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 45.3 ± 0.5 | 70.7 ± 0.5 | 136.0 | 304.0 |
| `Modify_6` | `-` | 110.4 ± 0.8 | 123.2 ± 3.8 | 184.0 | 504.0 |
| `Modify_firstQty` | `-` | 42.1 ± 0.4 | 13.5 ± 0.1 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 13.5 ± 0.1 | 16.2 ± 0.1 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.5 ± 0.0 | 7.1 ± 0.1 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 7.8 ± 0.1 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 31,735.7 ± 767.1 | — | 97,423.5 | — |
| `ModifyNames` | `size=64` | 4,012.6 ± 84.5 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 559.7 ± 11.7 | — | 1,984.0 | — |
| `ModifyStreet` | `size=512` | 111.4 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 110.8 ± 1.0 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 110.6 ± 0.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.5 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.1 ± 0.7 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.4 ± 0.7 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 92,010.7 ± 997.7 | — | 382,775.1 | — |
| `monocleModifyNames` | `size=64` | 8,911.1 ± 74.5 | — | 39,856.3 | — |
| `monocleModifyNames` | `size=8` | 1,412.5 ± 11.8 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 43,533.5 ± 1,626.6 | — | 169,078.7 | — |
| `monocleModifyStreet` | `size=64` | 6,000.6 ± 52.1 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 925.5 ± 11.7 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 25,064.1 ± 178.8 | — | 69,783.4 | — |
| `monocleReadStreet` | `size=64` | 3,222.3 ± 27.3 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 498.6 ± 44.0 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 60,005.5 ± 653.8 | — | 226,296.4 | — |
| `naiveModifyNames` | `size=64` | 7,627.9 ± 77.2 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,087.2 ± 17.1 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 46,053.0 ± 401.1 | — | 169,056.9 | — |
| `naiveModifyStreet` | `size=64` | 6,007.7 ± 78.7 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 922.7 ± 6.0 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,015.4 ± 258.6 | — | 69,775.3 | — |
| `naiveReadStreet` | `size=64` | 3,237.1 ± 31.8 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 478.9 ± 4.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 247,687.4 ± 2,008.4 | — | 626,350.5 | — |
| `Names` | `size=64` | 30,056.9 ± 245.5 | — | 79,299.2 | — |
| `Names` | `size=8` | 4,207.6 ± 35.0 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 271,477.2 ± 16,071.8 | — | 695,873.6 | — |
| `NamesIor` | `size=64` | 32,736.3 ± 381.0 | — | 87,491.2 | — |
| `NamesIor` | `size=8` | 4,583.6 ± 175.4 | — | 11,488.1 | — |
| `Street` | `size=512` | 1,074.0 ± 21.5 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,055.9 ± 18.7 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,068.9 ± 20.2 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,077.1 ± 18.5 | — | 2,736.9 | — |
| `StreetIor` | `size=64` | 1,072.2 ± 11.3 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,066.2 ± 13.2 | — | 2,736.0 | — |
| `directNames` | `size=512` | 241,380.8 ± 6,886.1 | — | 613,993.5 | — |
| `directNames` | `size=64` | 29,780.1 ± 717.9 | — | 77,187.0 | — |
| `directNames` | `size=8` | 4,159.5 ± 81.3 | — | 10,616.1 | — |
| `directStreet` | `size=512` | 1,061.4 ± 27.3 | — | 2,728.8 | — |
| `directStreet` | `size=64` | 1,054.3 ± 35.8 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 1,038.8 ± 11.2 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 242,899.6 ± 5,560.2 | — | 613,994.7 | — |
| `hcursorNames` | `size=64` | 30,387.2 ± 683.8 | — | 77,272.5 | — |
| `hcursorNames` | `size=8` | 4,111.7 ± 82.1 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,140.4 ± 40.6 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,155.0 ± 18.4 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,124.3 ± 20.1 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 226,041.3 ± 4,093.1 | — | 1,121,759.9 | — |
| `monocleNames` | `size=64` | 24,076.5 ± 305.3 | — | 132,765.0 | — |
| `monocleNames` | `size=8` | 3,642.6 ± 23.5 | — | 19,466.7 | — |
| `monocleStreet` | `size=512` | 177,843.8 ± 1,425.6 | — | 908,023.5 | — |
| `monocleStreet` | `size=64` | 20,685.4 ± 237.5 | — | 113,481.8 | — |
| `monocleStreet` | `size=8` | 3,146.9 ± 18.8 | — | 17,053.4 | — |
| `naiveNames` | `size=512` | 193,467.1 ± 3,352.3 | — | 965,266.0 | — |
| `naiveNames` | `size=64` | 22,679.4 ± 96.2 | — | 120,837.1 | — |
| `naiveNames` | `size=8` | 3,342.5 ± 22.4 | — | 17,813.4 | — |
| `naiveStreet` | `size=512` | 178,348.0 ± 2,343.3 | — | 908,029.1 | — |
| `naiveStreet` | `size=64` | 21,014.9 ± 465.9 | — | 113,436.5 | — |
| `naiveStreet` | `size=8` | 3,149.9 ± 29.9 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,907.7 ± 46.3 | — | 42,027.5 | — |
| `ModifyStreet` | `size=64` | 645.1 ± 9.6 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 300.8 ± 13.9 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 197.4 ± 4.5 | — | 109.5 | — |
| `ReadStreet` | `size=64` | 195.6 ± 2.0 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 198.0 ± 4.7 | — | 72.0 | — |
| `SumPrices` | `size=512` | 83,960.6 ± 724.8 | — | 63,713.4 | — |
| `SumPrices` | `size=64` | 11,048.5 ± 290.8 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,493.2 ± 38.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 157,665.0 ± 1,003.2 | — | 333,493.7 | — |
| `monocleModifyStreet` | `size=64` | 18,874.9 ± 172.4 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,159.4 ± 18.7 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 88,511.9 ± 1,167.6 | — | 193,227.8 | — |
| `monocleReadStreet` | `size=64` | 11,290.4 ± 137.3 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,788.5 ± 15.4 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 418,798.1 ± 2,730.8 | — | 1,178,439.3 | — |
| `monocleSumPrices` | `size=64` | 15,173.7 ± 217.7 | — | 45,817.6 | — |
| `monocleSumPrices` | `size=8` | 2,433.6 ± 22.9 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 157,913.6 ± 1,573.2 | — | 333,563.9 | — |
| `naiveModifyStreet` | `size=64` | 18,962.1 ± 205.0 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,139.2 ± 31.1 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 88,969.1 ± 865.2 | — | 193,228.2 | — |
| `naiveReadStreet` | `size=64` | 11,299.1 ± 283.7 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,776.8 ± 10.4 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 93,091.0 ± 1,299.3 | — | 230,119.8 | — |
| `naiveSumPrices` | `size=64` | 12,040.0 ± 253.1 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,904.0 ± 56.8 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,222.9 ± 426.4 | — | 451.6 | — |
| `nativeReadStreet` | `size=64` | 4,292.6 ± 95.0 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 818.3 ± 23.1 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 62,083.5 ± 686.9 | — | 86,271.0 | — |
| `nativeSumPrices` | `size=64` | 7,660.8 ± 36.8 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,155.5 ± 27.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 140,616.2 ± 10,596.3 | — | 455,006.3 | — |
| `TransformDeep` | `n=512` | 7,546.0 ± 277.6 | — | 20,520.8 | — |
| `TransformDeep` | `n=64` | 724.7 ± 8.6 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 93,636.2 ± 6,858.4 | 163,164.9 ± 917.0 | 360,516.2 | 753,734.8 |
| `TransformExpr` | `n=512` | 11,698.6 ± 121.1 | 15,493.3 ± 189.0 | 45,057.2 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,360.7 ± 6.1 | 2,541.5 ± 56.7 | 5,632.0 | 11,728.0 |
| `TransformMDeep` | `n=4096` | 165,811.2 ± 1,435.4 | — | 764,368.7 | — |
| `TransformMDeep` | `n=512` | 18,906.0 ± 319.3 | — | 82,145.9 | — |
| `TransformMDeep` | `n=64` | 1,806.8 ± 14.1 | — | 7,160.0 | — |
| `TransformMExpr` | `n=4096` | 172,399.4 ± 2,104.8 | 203,517.1 ± 1,449.2 | 599,578.8 | 837,988.1 |
| `TransformMExpr` | `n=512` | 22,213.3 ± 1,094.7 | 22,753.1 ± 2,261.5 | 70,778.3 | 97,322.3 |
| `TransformMExpr` | `n=64` | 2,657.1 ± 28.0 | 2,987.3 ± 216.0 | 9,213.4 | 13,200.1 |
| `UniverseDeep` | `n=4096` | 88,563.8 ± 786.6 | — | 360,688.5 | — |
| `UniverseDeep` | `n=512` | 10,831.6 ± 93.0 | — | 45,185.1 | — |
| `UniverseDeep` | `n=64` | 1,757.5 ± 19.4 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 80,696.5 ± 1,626.1 | 2,903,164.2 ± 203,313.1 | 295,034.7 | 4,687,790.2 |
| `UniverseExpr` | `n=512` | 9,893.5 ± 103.6 | 198,828.2 ± 2,650.6 | 36,929.0 | 475,012.4 |
| `UniverseExpr` | `n=64` | 1,447.4 ± 10.2 | 17,950.8 ± 578.5 | 4,640.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 197,811.1 ± 1,207.0 | 3,205,573.9 ± 128,577.4 | 393,400.0 | 6,489,872.5 |
| `UniverseJson` | `n=512` | 22,890.7 ± 138.0 | 241,583.0 ± 7,513.9 | 49,194.3 | 699,920.8 |
| `UniverseJson` | `n=64` | 2,913.0 ± 20.7 | 22,400.2 ± 719.0 | 6,152.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,245.2 ± 488.0 | — | 163,886.8 | — |
| `visitorTransformDeep` | `n=512` | 4,903.0 ± 32.1 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 431.6 ± 12.0 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,152.5 ± 1,035.8 | — | 360,470.7 | — |
| `visitorTransformExpr` | `n=512` | 7,573.4 ± 89.0 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 983.6 ± 10.1 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 66,352.8 ± 11,062.3 | — | 338,693.6 | — |
| `visitorTransformMDeep` | `n=512` | 6,257.7 ± 61.6 | — | 28,704.6 | — |
| `visitorTransformMDeep` | `n=64` | 579.0 ± 6.2 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 87,090.9 ± 693.2 | — | 447,863.4 | — |
| `visitorTransformMExpr` | `n=512` | 10,147.8 ± 36.3 | — | 50,489.0 | — |
| `visitorTransformMExpr` | `n=64` | 1,337.2 ± 15.0 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,473.7 ± 540.8 | — | 196,703.7 | — |
| `visitorUniverseDeep` | `n=512` | 6,478.5 ± 115.3 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 744.6 ± 3.9 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,618.0 ± 289.5 | — | 196,654.3 | — |
| `visitorUniverseExpr` | `n=512` | 6,522.7 ± 24.3 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 797.5 ± 6.8 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 102,834.1 ± 1,384.7 | — | 196,706.8 | — |
| `visitorUniverseJson` | `n=512` | 9,234.0 ± 75.1 | — | 24,600.9 | — |
| `visitorUniverseJson` | `n=64` | 1,347.9 ± 13.0 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,126.5 ± 108.2 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 259.2 ± 3.2 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,020.9 ± 64.6 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 123.0 ± 0.6 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,297.9 ± 1,940.8 | — | 164,371.5 | — |
| `Modify_powerEach` | `size=64` | 791.6 ± 11.0 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 61,675.0 ± 2,026.4 | — | 279,434.0 | — |
| `monocle_powerEach` | `size=16` | 609.9 ± 17.2 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 22,284.2 ± 1,079.5 | — | 107,331.6 | — |
| `monocle_powerEach` | `size=4` | 209.1 ± 2.1 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 176,334.3 ± 876.5 | — | 967,839.1 | — |
| `monocle_powerEach` | `size=64` | 2,150.3 ± 8.6 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,710.2 ± 29.0 | — | 28,731.1 | — |
| `naive_powerEach` | `size=16` | 100.9 ± 0.5 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,614.1 ± 8.3 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.0 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,791.1 ± 147.1 | — | 114,787.6 | — |
| `naive_powerEach` | `size=64` | 397.3 ± 1.9 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 73,012.3 ± 1,111.7 | — | 210,538.2 | — |
| `Modify_nested` | `size=16` | 1,441.3 ± 18.8 | — | 4,744.1 | — |
| `Modify_nested` | `size=256` | 17,228.5 ± 269.2 | — | 53,730.8 | — |
| `Modify_nested` | `size=4` | 689.3 ± 22.6 | — | 2,240.0 | — |
| `Modify_nested` | `size=64` | 4,577.8 ± 64.5 | — | 14,616.5 | — |
| `monocle_nested` | `size=1024` | 245,938.7 ± 2,280.7 | — | 1,118,847.7 | — |
| `monocle_nested` | `size=16` | 2,646.7 ± 24.2 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 92,734.1 ± 2,033.7 | — | 430,209.2 | — |
| `monocle_nested` | `size=4` | 1,295.7 ± 117.6 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 9,013.7 ± 120.5 | — | 58,833.0 | — |
| `naive_nested` | `size=1024` | 21,546.7 ± 163.0 | — | 115,071.2 | — |
| `naive_nested` | `size=16` | 364.1 ± 3.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,217.5 ± 36.5 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 125.9 ± 2.2 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,317.5 ± 11.6 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,393.6 ± 110.2 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,332.5 ± 977.6 | — | 104,682.6 | — |
| `Modify_sparse` | `size=32` | 345.5 ± 3.3 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,652.0 ± 39.3 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 118.9 ± 0.6 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,850.8 ± 33.4 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 106,695.7 ± 1,091.4 | — | 523,146.8 | — |
| `monocle_sparse` | `size=32` | 1,060.9 ± 17.4 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,141.4 ± 332.2 | — | 166,685.8 | — |
| `monocle_sparse` | `size=8` | 352.9 ± 15.2 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 287.8 ± 3.0 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,930.2 ± 42.6 | — | 24,612.3 | — |
| `naive_sparse` | `size=32` | 74.2 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,158.9 ± 6.8 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 24.9 ± 0.2 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.4 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.0 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.6 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 10.9 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 96,025.1 ± 1,175.8 | — | 589,712.7 | — |
| `Cata` | `-` | 60,430.8 ± 930.1 | — | 99,288.4 | — |
| `Hylo` | `-` | 89,604.9 ± 1,282.6 | — | 295,848.6 | — |
| `drosteAna` | `-` | 47,189.4 ± 1,010.6 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,220.1 ± 329.8 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 64,508.7 ± 639.5 | — | 328,640.5 | — |
| `handAna` | `-` | 20,480.9 ± 258.4 | — | 163,816.1 | — |
| `handCata` | `-` | 12,665.7 ± 107.1 | — | 0.1 | — |
| `handHylo` | `-` | 9,579.9 ± 62.6 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.2 ± 0.1 | 28.2 ± 0.3 | 72.0 | 168.0 |
| `Modify_6` | `-` | 24.0 ± 0.6 | 65.1 ± 1.6 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.2 ± 0.0 | 3.2 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,022.4 ± 62.3 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 533.3 ± 5.0 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 51.5 ± 4.2 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,872.5 ± 34.2 | 54,376.9 ± 38,633.0 | 12,312.3 | 162,568.6 |
| `FoldPrices` | `size=64` | 155.5 ± 1.5 | 2,065.2 ± 19.2 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.4 ± 0.1 | 324.8 ± 1.2 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,209.6 ± 204.7 | 33,565.4 ± 877.9 | 36,897.1 | 176,890.9 |
| `Modify` | `size=64` | 839.5 ± 3.0 | 1,781.2 ± 16.4 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 94.9 ± 0.7 | 283.3 ± 2.7 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.4 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 95.0 ± 1.1 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.3 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 64.7 ± 0.6 | — | 400.0 | — |
| `handDrillGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 89.9 ± 0.6 | — | 592.0 | — |
| `handServiceGet` | `-` | 14.1 ± 0.3 | — | 120.0 | — |
| `handServiceReplace` | `-` | 74.2 ± 0.3 | — | 520.0 | — |


# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `fd626af784e6e2d223297ead52b17b9673aa4e2e` · date: `2026-11-06` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.9 ± 0.1 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 13.5 ± 0.3 | 9.5 ± 0.2 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 26.1 ± 0.5 | 23.9 ± 0.2 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 153.0 ± 1.8 | — | 720.0 | — |
| `ModifyCountry` | `-` | 321.0 ± 5.3 | — | 944.0 | — |
| `ModifyPartner` | `-` | 401.4 ± 9.7 | — | 984.0 | — |
| `ReadCountry` | `-` | 178.7 ± 3.4 | — | 520.0 | — |
| `ReadPartner` | `-` | 211.5 ± 3.2 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 311.7 ± 2.8 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,690.0 ± 30.8 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 3,061.5 ± 29.0 | — | 8,696.1 | — |
| `naivePassthroughPayload` | `-` | 5,093.1 ± 28.3 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,647.3 ± 23.7 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,062.2 ± 16.9 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 908.5 ± 7.3 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 672.9 ± 4.1 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 374.3 ± 5.4 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 418.0 ± 8.3 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,513.7 ± 12.4 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,423.4 ± 12.9 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 57.3 ± 0.3 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 660.9 ± 5.6 | — | 568.0 | — |
| `handwritten_stream` | `-` | 168.8 ± 7.4 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 755.9 ± 2.8 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,052.0 ± 69.2 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,909.4 ± 10.1 | — | 3,984.0 | — |
| `WideToAvro` | `-` | 863.5 ± 11.3 | — | 2,040.0 | — |
| `WideToJson` | `-` | 677.8 ± 12.3 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,541.1 ± 11.4 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 3,152.3 ± 65.9 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 1,068.9 ± 18.1 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,934.7 ± 18.6 | — | 4,376.0 | — |

## AvroJsonDefaultsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `circeDefaultsFull` | `-` | 371.8 ± 6.8 | — | 1,189.3 | — |
| `circeDefaultsSparse` | `-` | 310.6 ± 22.8 | — | 698.7 | — |
| `circeStrictFull` | `-` | 375.3 ± 7.6 | — | 1,202.7 | — |
| `circeStrictSparse` | `-` | 3.2 ± 0.0 | — | 0.0 | — |
| `circeWithDefaults` | `-` | 736.9 ± 30.8 | — | 2,456.1 | — |
| `jsoniterDefaultsFull` | `-` | 1,403.1 ± 13.7 | — | 4,272.0 | — |
| `jsoniterDefaultsSparse` | `-` | 357.9 ± 7.8 | — | 672.0 | — |
| `jsoniterStrictFull` | `-` | 1,404.6 ± 11.3 | — | 4,272.0 | — |
| `jsoniterStrictSparse` | `-` | 550.4 ± 8.6 | — | 2,920.0 | — |
| `jsoniterWithDefaults` | `-` | 733.5 ± 6.3 | — | 1,048.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 204.7 ± 1.0 | — | 832.0 | — |
| `decode_native` | `-` | 17.2 ± 0.3 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 203.4 ± 0.6 | — | 832.0 | — |
| `encode_bridged` | `-` | 255.2 ± 2.1 | — | 1,224.0 | — |
| `encode_native` | `-` | 12.6 ± 0.3 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 254.9 ± 2.0 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 98.1 ± 1.1 | — | 437.3 | — |
| `fieldGet_native` | `-` | 101.6 ± 1.2 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 404.8 ± 4.4 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 174.0 ± 5.3 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 17.8 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 18.6 ± 0.3 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 17.6 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 17.4 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.2 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 5.2 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.3 ± 0.7 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 35.5 ± 0.9 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.9 ± 0.0 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 5.6 ± 0.1 | — | 40.0 | — |
| `modifyDirect` | `-` | 5.0 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 29.9 ± 0.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 27.2 ± 0.2 | — | 160.0 | — |
| `GetterRead` | `-` | 14.0 ± 0.6 | — | 48.0 | — |
| `Patch` | `-` | 7.0 ± 0.0 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 17.3 ± 0.7 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 25.8 ± 0.2 | — | 160.0 | — |
| `TransformFocus` | `-` | 993.4 ± 10.6 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,238.2 ± 9.9 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 1,183.0 ± 29.2 | — | 7,317.3 | — |
| `TransformFocusWrite` | `-` | 8.9 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 21.5 ± 0.3 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 24.6 ± 0.2 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.9 ± 0.3 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.8 ± 0.2 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 13.4 ± 0.3 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 6.3 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.7 ± 0.4 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 25.8 ± 0.2 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 935.5 ± 11.6 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 1,070.4 ± 3.3 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,701.1 ± 23.2 | — | 10,578.7 | — |
| `chimneyTransformFocusWrite` | `-` | 6.2 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 27.6 ± 0.1 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.6 ± 0.1 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 22.9 ± 0.2 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.5 ± 0.3 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 6.4 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 497.9 ± 4.1 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 621.0 ± 3.1 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 434.2 ± 2.4 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 6.5 ± 0.1 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.8 ± 0.2 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 8.8 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 1.2 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 498.6 ± 4.8 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 494.4 ± 3.4 | — | 744.0 | — |
| `encode_hand` | `-` | 173.8 ± 0.5 | — | 768.0 | — |
| `encode_positional` | `-` | 5,484.6 ± 35.3 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 6,402.9 ± 101.2 | — | 28,408.0 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 9.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 27.4 ± 0.2 | — | 184.0 | — |
| `buildLens6` | `-` | 56.1 ± 0.3 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 36.6 ± 0.1 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 15.8 ± 0.1 | — | 40.0 | — |
| `reuseLens3` | `-` | 40.1 ± 0.5 | — | 72.0 | — |
| `reuseLens6` | `-` | 112.0 ± 1.5 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 41.5 ± 0.2 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,333.8 ± 20.6 | 1,333.9 ± 25.8 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 156.6 ± 0.5 | 156.0 ± 0.6 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 17.3 ± 0.1 | 17.2 ± 0.2 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,403.2 ± 29.5 | 2,406.3 ± 29.7 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 204.6 ± 0.7 | 203.6 ± 1.2 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 28.4 ± 0.3 | 28.2 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 4.0 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.5 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.4 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 3.4 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 2.3 ± 0.1 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 1.0 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 14.0 ± 0.2 | 7.4 ± 0.4 | 0.0 | 0.0 |
| `Get_6` | `-` | 27.2 ± 0.6 | 22.6 ± 1.7 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 1.0 ± 0.0 | 0.6 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 4.4 ± 0.0 | 4.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.9 ± 0.0 | 4.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 401,159.0 ± 5,477.3 | — | 1,066,760.8 | — |
| `cModifyId` | `size=64` | 52,001.1 ± 405.6 | — | 136,293.8 | — |
| `cModifyId` | `size=8` | 8,329.2 ± 59.2 | — | 20,704.1 | — |
| `cReadId` | `size=512` | 205,653.1 ± 2,493.4 | — | 797,930.5 | — |
| `cReadId` | `size=64` | 26,158.0 ± 384.1 | — | 101,292.9 | — |
| `cReadId` | `size=8` | 3,924.1 ± 92.6 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 205,643.2 ± 1,841.4 | — | 797,930.5 | — |
| `cReadStreet` | `size=64` | 26,020.9 ± 412.6 | — | 101,292.8 | — |
| `cReadStreet` | `size=8` | 3,958.0 ± 45.6 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 402,669.6 ± 5,191.1 | — | 1,066,700.3 | — |
| `cReplaceId` | `size=64` | 51,470.7 ± 318.2 | — | 136,237.4 | — |
| `cReplaceId` | `size=8` | 8,295.4 ± 46.2 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 342,391.9 ± 4,919.7 | — | 1,240,737.9 | — |
| `cSumPrices` | `size=64` | 42,704.2 ± 288.8 | — | 157,069.6 | — |
| `cSumPrices` | `size=8` | 6,208.5 ± 77.5 | — | 22,682.7 | — |
| `jMiss` | `size=512` | 168.6 ± 1.4 | — | 0.0 | — |
| `jMiss` | `size=64` | 170.0 ± 0.8 | — | 0.0 | — |
| `jMiss` | `size=8` | 166.2 ± 0.6 | — | 0.0 | — |
| `jModifyId` | `size=512` | 5,502.7 ± 72.3 | — | 41,929.6 | — |
| `jModifyId` | `size=64` | 644.8 ± 11.3 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 142.3 ± 1.1 | — | 992.0 | — |
| `jReadId` | `size=512` | 37.1 ± 2.3 | — | 56.0 | — |
| `jReadId` | `size=64` | 35.2 ± 1.9 | — | 48.0 | — |
| `jReadId` | `size=8` | 35.4 ± 1.3 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 185.2 ± 2.3 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 185.8 ± 4.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 186.1 ± 2.7 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 5,489.9 ± 49.3 | — | 41,889.6 | — |
| `jReplaceId` | `size=64` | 638.7 ± 5.7 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 140.9 ± 5.0 | — | 952.0 | — |
| `jSumPrices` | `size=512` | 83,463.4 ± 1,219.5 | — | 63,664.9 | — |
| `jSumPrices` | `size=64` | 10,796.4 ± 167.5 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,498.1 ± 38.2 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 106.1 ± 1.5 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.8 ± 0.4 | — | 216.0 | — |
| `MapGet` | `-` | 2.5 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 75.9 ± 0.8 | — | 216.0 | — |
| `handEnvUse` | `-` | 104.0 ± 1.1 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 35.0 ± 0.5 | — | 216.0 | — |
| `handMapGet` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 72.8 ± 3.4 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.2 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 5.0 ± 0.0 | 5.5 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 35.3 ± 2.0 | 29.9 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 4.7 ± 0.1 | 4.7 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 15,984.5 ± 870.6 | — | 43,036.1 | — |
| `Fold_powerEach` | `size=256` | 3,222.8 ± 18.5 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 375.2 ± 2.7 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 76.5 ± 0.5 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 60,378.7 ± 1,758.2 | — | 331,134.7 | — |
| `Modify_multiFocus` | `size=256` | 13,179.1 ± 56.1 | — | 76,112.9 | — |
| `Modify_multiFocus` | `size=32` | 1,569.9 ± 30.9 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 232.0 ± 2.2 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,427.9 ± 576.7 | — | 115,177.2 | — |
| `Modify_powerEach` | `size=256` | 7,917.0 ± 53.6 | — | 26,080.5 | — |
| `Modify_powerEach` | `size=32` | 1,004.1 ± 6.7 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 191.0 ± 2.0 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 9,869.8 ± 80.2 | — | 65,578.6 | — |
| `naive_listMap` | `size=256` | 2,408.7 ± 34.0 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 296.8 ± 4.1 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 42.8 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,686.5 ± 132.7 | — | 16,129.2 | — |
| `naive_sumQty` | `size=256` | 668.8 ± 4.7 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 66.9 ± 0.6 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 6.8 ± 0.1 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 34.3 ± 0.2 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 134.7 ± 0.4 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 12.9 ± 0.2 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 34.4 ± 0.3 | — | 248.0 | — |
| `naive_constSum` | `-` | 2.5 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 31.0 ± 0.9 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 12.4 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 22.6 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 202.8 ± 0.6 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.9 ± 1.1 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,070.0 ± 23.2 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,012.7 ± 14.5 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.1 ± 0.2 | 22.0 ± 0.1 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.2 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 41.4 ± 0.2 | 67.1 ± 0.3 | 136.0 | 304.0 |
| `Modify_6` | `-` | 98.9 ± 0.5 | 113.0 ± 4.3 | 184.0 | 496.0 |
| `Modify_firstQty` | `-` | 40.6 ± 0.5 | 15.5 ± 0.1 | 160.0 | 104.0 |
| `Modify_loyalty` | `-` | 15.5 ± 0.4 | 18.4 ± 0.4 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.3 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 6.0 ± 0.0 | 5.8 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 10.9 ± 0.4 | 10.9 ± 0.2 | 88.0 | 88.0 |
| `handModify_firstQty` | `-` | 12.4 ± 0.2 | — | 104.0 | — |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,641.3 ± 474.5 | — | 97,423.7 | — |
| `ModifyNames` | `size=64` | 3,999.8 ± 17.2 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 550.3 ± 3.6 | — | 1,984.0 | — |
| `ModifyStreet` | `size=512` | 121.7 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 121.4 ± 0.8 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 121.7 ± 0.8 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.5 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.2 ± 0.3 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.3 ± 0.6 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 92,775.0 ± 538.8 | — | 382,776.3 | — |
| `monocleModifyNames` | `size=64` | 9,469.5 ± 67.6 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,459.7 ± 6.2 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 46,059.8 ± 387.6 | — | 169,080.1 | — |
| `monocleModifyStreet` | `size=64` | 5,988.4 ± 20.1 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 915.0 ± 14.9 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 24,829.7 ± 94.7 | — | 69,782.7 | — |
| `monocleReadStreet` | `size=64` | 3,146.4 ± 12.2 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 471.4 ± 2.6 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 56,977.3 ± 1,644.6 | — | 226,294.7 | — |
| `naiveModifyNames` | `size=64` | 7,542.9 ± 54.5 | — | 27,920.3 | — |
| `naiveModifyNames` | `size=8` | 1,104.8 ± 23.8 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 46,271.2 ± 577.0 | — | 169,057.1 | — |
| `naiveModifyStreet` | `size=64` | 5,973.3 ± 21.8 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 923.4 ± 11.5 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,020.7 ± 413.7 | — | 69,782.3 | — |
| `naiveReadStreet` | `size=64` | 3,139.1 ± 7.7 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 479.0 ± 7.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 231,953.9 ± 1,890.8 | — | 626,337.9 | — |
| `Names` | `size=64` | 29,389.8 ± 312.9 | — | 78,259.1 | — |
| `Names` | `size=8` | 4,066.1 ± 31.5 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 255,965.8 ± 3,959.8 | — | 695,861.2 | — |
| `NamesIor` | `size=64` | 32,224.3 ± 269.1 | — | 87,499.4 | — |
| `NamesIor` | `size=8` | 4,246.5 ± 28.6 | — | 11,560.1 | — |
| `Street` | `size=512` | 1,019.3 ± 10.4 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,008.8 ± 15.4 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,020.1 ± 15.1 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,027.3 ± 19.6 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,010.6 ± 10.2 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,026.6 ± 19.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 238,457.3 ± 3,240.1 | — | 613,991.2 | — |
| `directNames` | `size=64` | 29,392.0 ± 338.6 | — | 77,704.9 | — |
| `directNames` | `size=8` | 3,985.5 ± 38.4 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,025.9 ± 16.0 | — | 2,742.1 | — |
| `directStreet` | `size=64` | 1,021.5 ± 4.6 | — | 2,736.1 | — |
| `directStreet` | `size=8` | 1,011.1 ± 16.4 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 232,482.5 ± 1,827.7 | — | 613,986.3 | — |
| `hcursorNames` | `size=64` | 29,482.6 ± 631.5 | — | 77,269.0 | — |
| `hcursorNames` | `size=8` | 4,045.8 ± 29.6 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,094.8 ± 9.3 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,094.4 ± 5.1 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,104.8 ± 19.9 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 223,363.8 ± 1,317.6 | — | 1,121,763.4 | — |
| `monocleNames` | `size=64` | 25,355.8 ± 489.3 | — | 132,751.5 | — |
| `monocleNames` | `size=8` | 3,869.9 ± 48.2 | — | 19,461.4 | — |
| `monocleStreet` | `size=512` | 172,441.9 ± 763.8 | — | 908,030.6 | — |
| `monocleStreet` | `size=64` | 21,638.9 ± 138.6 | — | 113,820.6 | — |
| `monocleStreet` | `size=8` | 3,299.4 ± 29.7 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 186,932.3 ± 1,851.8 | — | 965,261.6 | — |
| `naiveNames` | `size=64` | 23,232.5 ± 147.4 | — | 120,842.5 | — |
| `naiveNames` | `size=8` | 3,501.7 ± 32.2 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 170,768.9 ± 1,714.8 | — | 908,016.1 | — |
| `naiveStreet` | `size=64` | 21,342.8 ± 160.8 | — | 113,791.2 | — |
| `naiveStreet` | `size=8` | 3,290.6 ± 23.5 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 5,669.2 ± 43.5 | — | 42,029.1 | — |
| `ModifyStreet` | `size=64` | 828.1 ± 8.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 322.1 ± 3.7 | — | 1,080.0 | — |
| `ReadStreet` | `size=512` | 187.9 ± 2.6 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 184.1 ± 5.2 | — | 90.7 | — |
| `ReadStreet` | `size=8` | 180.9 ± 8.0 | — | 90.7 | — |
| `SumPrices` | `size=512` | 82,115.4 ± 858.0 | — | 63,714.7 | — |
| `SumPrices` | `size=64` | 10,660.8 ± 79.6 | — | 8,121.2 | — |
| `SumPrices` | `size=8` | 1,481.4 ± 85.1 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 158,829.8 ± 2,436.4 | — | 333,572.0 | — |
| `monocleModifyStreet` | `size=64` | 19,215.6 ± 123.0 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,330.4 ± 13.3 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 85,503.8 ± 1,508.9 | — | 193,225.2 | — |
| `monocleReadStreet` | `size=64` | 11,043.2 ± 90.3 | — | 24,705.1 | — |
| `monocleReadStreet` | `size=8` | 1,833.8 ± 8.5 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 393,346.9 ± 3,545.4 | — | 1,178,417.3 | — |
| `monocleSumPrices` | `size=64` | 15,555.7 ± 125.5 | — | 45,841.6 | — |
| `monocleSumPrices` | `size=8` | 2,568.9 ± 29.3 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 157,300.4 ± 1,659.8 | — | 333,563.4 | — |
| `naiveModifyStreet` | `size=64` | 19,320.6 ± 138.8 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,349.7 ± 9.8 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 85,397.5 ± 1,599.5 | — | 193,225.1 | — |
| `naiveReadStreet` | `size=64` | 11,091.0 ± 258.5 | — | 24,705.1 | — |
| `naiveReadStreet` | `size=8` | 1,821.6 ± 8.7 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 92,608.0 ± 1,228.0 | — | 230,119.3 | — |
| `naiveSumPrices` | `size=64` | 11,707.2 ± 245.4 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,946.6 ± 8.5 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 32,915.7 ± 371.5 | — | 451.5 | — |
| `nativeReadStreet` | `size=64` | 4,268.3 ± 104.3 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 784.1 ± 1.9 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 59,404.9 ± 481.4 | — | 86,272.5 | — |
| `nativeSumPrices` | `size=64` | 7,257.6 ± 27.8 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,130.6 ± 28.5 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 128,486.4 ± 1,939.2 | — | 472,925.5 | — |
| `TransformDeep` | `n=512` | 8,744.3 ± 39.9 | — | 20,520.9 | — |
| `TransformDeep` | `n=64` | 870.4 ± 5.8 | — | 2,600.0 | — |
| `TransformExpr` | `n=4096` | 97,887.7 ± 1,415.5 | 173,541.4 ± 2,098.6 | 360,519.2 | 753,742.3 |
| `TransformExpr` | `n=512` | 12,226.4 ± 288.8 | 16,217.5 ± 370.5 | 45,057.2 | 69,585.7 |
| `TransformExpr` | `n=64` | 1,501.9 ± 10.7 | 2,675.2 ± 17.9 | 5,632.0 | 11,728.1 |
| `TransformMDeep` | `n=4096` | 179,457.8 ± 3,027.3 | — | 782,298.6 | — |
| `TransformMDeep` | `n=512` | 20,241.7 ± 246.8 | — | 82,250.1 | — |
| `TransformMDeep` | `n=64` | 1,953.5 ± 53.3 | — | 7,138.7 | — |
| `TransformMExpr` | `n=4096` | 192,979.7 ± 2,135.2 | 187,812.1 ± 28,748.7 | 566,500.5 | 861,912.7 |
| `TransformMExpr` | `n=512` | 25,021.6 ± 1,329.3 | 25,869.9 ± 3,391.6 | 79,319.9 | 104,338.6 |
| `TransformMExpr` | `n=64` | 3,127.8 ± 86.0 | 2,708.8 ± 57.2 | 9,216.1 | 12,520.1 |
| `UniverseDeep` | `n=4096` | 99,068.2 ± 264.2 | — | 360,696.1 | — |
| `UniverseDeep` | `n=512` | 11,909.5 ± 38.0 | — | 45,185.2 | — |
| `UniverseDeep` | `n=64` | 1,685.5 ± 4.4 | — | 5,656.0 | — |
| `UniverseExpr` | `n=4096` | 90,950.7 ± 941.8 | 4,178,514.3 ± 131,156.8 | 295,042.2 | 4,688,715.0 |
| `UniverseExpr` | `n=512` | 10,852.5 ± 36.0 | 319,302.6 ± 4,365.4 | 36,929.1 | 475,024.8 |
| `UniverseExpr` | `n=64` | 1,428.6 ± 14.2 | 28,816.5 ± 320.5 | 4,640.0 | 45,424.6 |
| `UniverseJson` | `n=4096` | 193,611.4 ± 1,364.9 | 4,540,874.7 ± 161,516.6 | 393,396.9 | 6,490,839.4 |
| `UniverseJson` | `n=512` | 24,002.0 ± 219.9 | 349,000.8 ± 1,923.5 | 49,194.5 | 699,931.6 |
| `UniverseJson` | `n=64` | 2,823.5 ± 19.7 | 33,532.2 ± 994.9 | 6,152.1 | 73,208.7 |
| `visitorTransformDeep` | `n=4096` | 49,878.4 ± 210.3 | — | 163,892.3 | — |
| `visitorTransformDeep` | `n=512` | 5,769.1 ± 27.9 | — | 20,496.6 | — |
| `visitorTransformDeep` | `n=64` | 597.9 ± 4.6 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 70,483.6 ± 293.7 | — | 360,475.3 | — |
| `visitorTransformExpr` | `n=512` | 8,494.8 ± 52.9 | — | 45,032.9 | — |
| `visitorTransformExpr` | `n=64` | 1,070.5 ± 4.4 | — | 5,608.0 | — |
| `visitorTransformMDeep` | `n=4096` | 72,353.1 ± 11,381.4 | — | 284,079.3 | — |
| `visitorTransformMDeep` | `n=512` | 7,317.0 ± 325.8 | — | 28,704.7 | — |
| `visitorTransformMDeep` | `n=64` | 826.0 ± 50.6 | — | 3,616.0 | — |
| `visitorTransformMExpr` | `n=4096` | 88,868.9 ± 1,099.8 | — | 447,864.7 | — |
| `visitorTransformMExpr` | `n=512` | 11,531.3 ± 278.3 | — | 50,489.2 | — |
| `visitorTransformMExpr` | `n=64` | 1,353.1 ± 15.4 | — | 6,968.0 | — |
| `visitorUniverseDeep` | `n=4096` | 72,190.6 ± 204.9 | — | 196,716.5 | — |
| `visitorUniverseDeep` | `n=512` | 8,834.9 ± 27.7 | — | 24,632.9 | — |
| `visitorUniverseDeep` | `n=64` | 943.7 ± 7.5 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 53,795.3 ± 322.8 | — | 196,655.2 | — |
| `visitorUniverseExpr` | `n=512` | 6,588.9 ± 30.4 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 782.7 ± 2.9 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 94,956.9 ± 548.3 | — | 196,701.1 | — |
| `visitorUniverseJson` | `n=512` | 9,953.4 ± 89.8 | — | 24,601.0 | — |
| `visitorUniverseJson` | `n=64` | 1,397.1 ± 6.5 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,567.8 ± 112.7 | — | 41,414.3 | — |
| `Modify_powerEach` | `size=16` | 259.0 ± 2.3 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,326.4 ± 35.7 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 117.2 ± 0.3 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 55,403.1 ± 764.8 | — | 164,378.3 | — |
| `Modify_powerEach` | `size=64` | 855.2 ± 8.3 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 60,147.8 ± 534.7 | — | 279,432.9 | — |
| `monocle_powerEach` | `size=16` | 681.0 ± 2.0 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,969.0 ± 261.2 | — | 107,331.4 | — |
| `monocle_powerEach` | `size=4` | 209.8 ± 10.1 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 207,905.1 ± 3,321.8 | — | 967,890.5 | — |
| `monocle_powerEach` | `size=64` | 2,545.4 ± 10.5 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 5,576.7 ± 104.7 | — | 28,730.6 | — |
| `naive_powerEach` | `size=16` | 88.4 ± 0.6 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,277.4 ± 20.4 | — | 7,224.2 | — |
| `naive_powerEach` | `size=4` | 27.1 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 22,406.8 ± 150.7 | — | 114,780.5 | — |
| `naive_powerEach` | `size=64` | 327.5 ± 2.1 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 76,657.9 ± 1,018.2 | — | 210,528.9 | — |
| `Modify_nested` | `size=16` | 1,532.7 ± 36.3 | — | 4,744.1 | — |
| `Modify_nested` | `size=256` | 18,965.5 ± 120.9 | — | 53,739.9 | — |
| `Modify_nested` | `size=4` | 714.8 ± 2.4 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,818.6 ± 47.1 | — | 14,640.6 | — |
| `monocle_nested` | `size=1024` | 244,692.6 ± 1,961.4 | — | 1,118,845.5 | — |
| `monocle_nested` | `size=16` | 2,898.2 ± 15.2 | — | 15,696.1 | — |
| `monocle_nested` | `size=256` | 93,461.0 ± 1,231.1 | — | 430,209.8 | — |
| `monocle_nested` | `size=4` | 1,243.9 ± 28.3 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 10,354.3 ± 80.5 | — | 58,833.1 | — |
| `naive_nested` | `size=1024` | 21,903.8 ± 170.4 | — | 115,071.9 | — |
| `naive_nested` | `size=16` | 367.8 ± 2.9 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,439.7 ± 59.1 | — | 29,019.4 | — |
| `naive_nested` | `size=4` | 134.1 ± 1.7 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,296.9 ± 20.7 | — | 7,512.1 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,429.8 ± 5.7 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 25,224.5 ± 314.2 | — | 104,677.0 | — |
| `Modify_sparse` | `size=32` | 380.9 ± 1.8 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 6,121.9 ± 21.8 | — | 24,785.7 | — |
| `Modify_sparse` | `size=8` | 123.1 ± 1.0 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,647.7 ± 22.9 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 111,725.6 ± 1,876.9 | — | 523,149.7 | — |
| `monocle_sparse` | `size=32` | 1,215.8 ± 5.1 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,575.2 ± 200.0 | — | 166,685.4 | — |
| `monocle_sparse` | `size=8` | 378.3 ± 5.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 337.9 ± 2.5 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,780.0 ± 94.5 | — | 24,612.9 | — |
| `naive_sparse` | `size=32` | 87.6 ± 0.7 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,353.8 ± 3.6 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 24.6 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.2 ± 0.0 | 1.3 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.6 ± 0.0 | 2.8 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.6 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.6 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 3.2 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.0 ± 1.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 32.5 ± 1.4 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 3.2 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 10.4 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 17.9 ± 0.1 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 100,627.8 ± 1,401.0 | — | 589,712.7 | — |
| `Cata` | `-` | 56,520.8 ± 1,502.8 | — | 99,288.4 | — |
| `Hylo` | `-` | 85,622.9 ± 391.1 | — | 295,848.6 | — |
| `drosteAna` | `-` | 50,790.0 ± 173.3 | — | 327,632.4 | — |
| `drosteCata` | `-` | 39,141.1 ± 236.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 56,248.0 ± 233.6 | — | 328,640.4 | — |
| `handAna` | `-` | 27,458.7 ± 292.0 | — | 163,816.2 | — |
| `handCata` | `-` | 13,657.1 ± 75.7 | — | 0.1 | — |
| `handHylo` | `-` | 10,944.8 ± 56.3 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 3.3 ± 0.1 | 3.2 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 13.2 ± 0.5 | 29.3 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 26.8 ± 2.1 | 58.9 ± 1.2 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 4.6 ± 0.0 | 4.6 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,412.6 ± 39.8 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 621.2 ± 2.9 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 61.8 ± 5.1 | — | 392.0 | — |
| `FoldPrices` | `size=512` | 2,040.4 ± 22.1 | 31,566.2 ± 228.8 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 207.0 ± 2.9 | 2,586.4 ± 15.3 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 28.7 ± 0.2 | 377.3 ± 4.9 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 8,147.7 ± 102.6 | 34,406.6 ± 636.9 | 36,897.3 | 176,890.8 |
| `Modify` | `size=64` | 878.3 ± 3.8 | 2,362.2 ± 9.5 | 4,640.0 | 14,408.1 |
| `Modify` | `size=8` | 102.7 ± 0.3 | 345.4 ± 2.4 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 11.0 ± 0.1 | — | 0.0 | — |
| `DrillModify` | `-` | 111.3 ± 3.0 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.6 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 83.1 ± 0.2 | — | 400.0 | — |
| `handDrillGet` | `-` | 18.5 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 103.4 ± 0.4 | — | 592.0 | — |
| `handServiceGet` | `-` | 18.5 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 90.3 ± 0.5 | — | 520.0 | — |


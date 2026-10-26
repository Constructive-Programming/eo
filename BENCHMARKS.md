# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `570460441ab3fc1e118de3934a2aed2c206b64f1` · date: `2026-10-26` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 15.6 ± 0.7 | 10.2 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 29.0 ± 0.0 | 26.1 ± 0.5 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.1 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.1 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 155.6 ± 0.6 | — | 720.0 | — |
| `ModifyCountry` | `-` | 290.2 ± 5.1 | — | 928.0 | — |
| `ModifyPartner` | `-` | 350.0 ± 3.0 | — | 984.0 | — |
| `ReadCountry` | `-` | 182.0 ± 2.2 | — | 520.0 | — |
| `ReadPartner` | `-` | 206.6 ± 2.0 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 317.7 ± 10.9 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,649.6 ± 44.1 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 3,079.0 ± 54.9 | — | 8,696.1 | — |
| `naivePassthroughPayload` | `-` | 4,914.6 ± 76.9 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,653.7 ± 16.5 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,023.6 ± 33.1 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 876.2 ± 6.2 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 578.6 ± 9.2 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 417.9 ± 4.0 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 442.4 ± 1.6 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,366.8 ± 13.6 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,350.5 ± 14.2 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 55.2 ± 0.1 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 648.7 ± 5.8 | — | 568.0 | — |
| `handwritten_stream` | `-` | 156.9 ± 1.9 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 733.5 ± 67.5 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 3,029.9 ± 12.6 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,784.9 ± 34.2 | — | 3,968.0 | — |
| `WideToAvro` | `-` | 723.9 ± 3.0 | — | 2,040.0 | — |
| `WideToJson` | `-` | 574.7 ± 1.6 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,391.5 ± 25.4 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,965.5 ± 28.6 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 929.8 ± 8.1 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,879.6 ± 23.9 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 191.8 ± 5.1 | — | 832.0 | — |
| `decode_native` | `-` | 17.2 ± 0.2 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 182.7 ± 1.4 | — | 832.0 | — |
| `encode_bridged` | `-` | 227.0 ± 1.4 | — | 1,224.0 | — |
| `encode_native` | `-` | 12.2 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 226.9 ± 1.2 | — | 1,224.0 | — |
| `fieldGet_bridged` | `-` | 101.5 ± 0.5 | — | 432.0 | — |
| `fieldGet_native` | `-` | 102.2 ± 1.4 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 347.7 ± 2.1 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 168.0 ± 2.6 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.0 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 21.4 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 20.9 ± 0.0 | — | 0.0 | — |
| `foldMapDirect` | `-` | 20.9 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.3 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.1 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 30.5 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 36.9 ± 0.0 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.4 ± 0.4 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.0 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 32.3 ± 0.1 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 26.4 ± 0.1 | — | 160.0 | — |
| `GetterRead` | `-` | 13.4 ± 0.5 | — | 48.0 | — |
| `Patch` | `-` | 6.9 ± 0.0 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.5 ± 0.4 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 22.2 ± 0.0 | — | 160.0 | — |
| `TransformFocus` | `-` | 840.0 ± 4.6 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,134.5 ± 2.3 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 867.2 ± 3.7 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 9.0 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 23.4 ± 0.1 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 24.5 ± 0.2 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 37.6 ± 0.1 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 29.2 ± 0.0 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.1 ± 0.0 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.7 ± 0.0 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 21.8 ± 0.0 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 823.5 ± 3.1 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 992.8 ± 1.9 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,380.5 ± 9.8 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 27.4 ± 0.0 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 14.9 ± 0.0 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 20.3 ± 0.0 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.8 ± 0.0 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.8 ± 0.1 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 479.1 ± 2.2 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 405.8 ± 0.7 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 269.5 ± 0.9 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.9 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 19.1 ± 0.0 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.5 ± 0.0 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 587.4 ± 28.5 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 591.5 ± 19.7 | — | 744.0 | — |
| `encode_hand` | `-` | 142.5 ± 0.5 | — | 768.0 | — |
| `encode_positional` | `-` | 6,596.2 ± 53.5 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 8,055.9 ± 107.6 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.2 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.9 ± 0.0 | — | 184.0 | — |
| `buildLens6` | `-` | 40.6 ± 0.0 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 26.3 ± 0.0 | — | 240.0 | — |
| `reuseLeaf` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 17.1 ± 0.9 | — | 40.0 | — |
| `reuseLens3` | `-` | 48.7 ± 1.8 | — | 72.0 | — |
| `reuseLens6` | `-` | 134.3 ± 0.2 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 47.6 ± 0.3 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,350.0 ± 8.3 | 1,348.0 ± 15.2 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 162.0 ± 2.8 | 160.8 ± 0.4 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 20.9 ± 0.0 | 20.5 ± 0.0 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,390.3 ± 68.5 | 2,408.8 ± 57.5 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 153.1 ± 0.3 | 153.4 ± 1.0 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 21.2 ± 0.0 | 21.1 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.7 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.3 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.1 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.9 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.7 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.5 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.0 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 18.0 ± 0.0 | 9.4 ± 0.2 | 0.0 | 0.0 |
| `Get_6` | `-` | 31.7 ± 0.0 | 25.5 ± 0.7 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.7 ± 0.0 | 4.0 ± 0.1 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.3 ± 0.0 | 3.5 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 426,247.2 ± 5,524.9 | — | 1,066,772.7 | — |
| `cModifyId` | `size=64` | 54,876.7 ± 281.6 | — | 136,294.4 | — |
| `cModifyId` | `size=8` | 9,017.9 ± 99.3 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 210,964.7 ± 1,301.3 | — | 797,932.0 | — |
| `cReadId` | `size=64` | 26,911.2 ± 133.6 | — | 101,295.2 | — |
| `cReadId` | `size=8` | 4,109.6 ± 43.7 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 210,896.9 ± 597.6 | — | 797,932.0 | — |
| `cReadStreet` | `size=64` | 27,042.3 ± 288.8 | — | 101,295.3 | — |
| `cReadStreet` | `size=8` | 4,101.5 ± 10.0 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 428,002.3 ± 3,479.4 | — | 1,066,715.2 | — |
| `cReplaceId` | `size=64` | 55,042.6 ± 366.2 | — | 136,246.7 | — |
| `cReplaceId` | `size=8` | 8,937.8 ± 90.3 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 343,468.7 ± 1,956.4 | — | 1,240,741.7 | — |
| `cSumPrices` | `size=64` | 42,572.9 ± 110.2 | — | 157,020.2 | — |
| `cSumPrices` | `size=8` | 6,305.5 ± 148.0 | — | 22,645.4 | — |
| `jMiss` | `size=512` | 189.1 ± 4.7 | — | 0.1 | — |
| `jMiss` | `size=64` | 189.8 ± 3.7 | — | 0.0 | — |
| `jMiss` | `size=8` | 186.8 ± 3.1 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,060.2 ± 8.2 | — | 41,920.9 | — |
| `jModifyId` | `size=64` | 280.7 ± 2.7 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 102.7 ± 0.4 | — | 976.0 | — |
| `jReadId` | `size=512` | 35.8 ± 0.7 | — | 48.0 | — |
| `jReadId` | `size=64` | 38.3 ± 3.9 | — | 56.0 | — |
| `jReadId` | `size=8` | 37.9 ± 1.7 | — | 56.0 | — |
| `jReadStreet` | `size=512` | 208.5 ± 1.3 | — | 128.1 | — |
| `jReadStreet` | `size=64` | 208.6 ± 2.3 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 207.5 ± 1.3 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 3,062.1 ± 39.6 | — | 41,896.9 | — |
| `jReplaceId` | `size=64` | 277.7 ± 4.8 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 98.8 ± 2.5 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 88,869.3 ± 338.4 | — | 63,660.7 | — |
| `jSumPrices` | `size=64` | 11,139.2 ± 85.9 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,477.8 ± 21.1 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 92.7 ± 11.0 | — | 320.0 | — |
| `MapDrillModify` | `-` | 41.6 ± 0.3 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 69.9 ± 0.8 | — | 216.0 | — |
| `handEnvUse` | `-` | 83.5 ± 0.9 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 37.7 ± 0.5 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 66.1 ± 1.1 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.0 ± 0.0 | 4.4 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 37.0 ± 0.1 | 30.8 ± 0.2 | 152.0 | 176.0 |
| `Replace` | `-` | 3.5 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 16,580.5 ± 932.0 | — | 43,036.3 | — |
| `Fold_powerEach` | `size=256` | 3,571.6 ± 123.6 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 383.7 ± 1.1 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 77.7 ± 0.3 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 55,470.9 ± 1,525.9 | — | 331,115.5 | — |
| `Modify_multiFocus` | `size=256` | 13,107.1 ± 491.7 | — | 76,134.2 | — |
| `Modify_multiFocus` | `size=32` | 1,386.1 ± 8.4 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 204.2 ± 1.2 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 37,937.1 ± 647.0 | — | 115,177.8 | — |
| `Modify_powerEach` | `size=256` | 8,404.9 ± 1,024.5 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,026.9 ± 7.1 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 185.1 ± 0.3 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 9,811.9 ± 45.4 | — | 65,578.5 | — |
| `naive_listMap` | `size=256` | 2,450.2 ± 8.4 | — | 16,424.2 | — |
| `naive_listMap` | `size=32` | 269.0 ± 3.7 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 35.1 ± 0.7 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,848.5 ± 138.7 | — | 16,129.3 | — |
| `naive_sumQty` | `size=256` | 901.6 ± 49.2 | — | 3,840.1 | — |
| `naive_sumQty` | `size=32` | 64.4 ± 0.4 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 7.2 ± 0.0 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 35.3 ± 0.5 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 132.5 ± 0.2 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.6 ± 0.0 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 26.9 ± 0.0 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 33.2 ± 0.7 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.0 ± 0.0 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.8 ± 0.0 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 159.7 ± 0.1 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 47.4 ± 0.5 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,041.6 ± 18.1 | — | 2,888.0 | — |
| `reuseUse` | `-` | 987.0 ± 10.7 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 21.4 ± 0.0 | 23.0 ± 0.1 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.1 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 47.4 ± 0.5 | 67.5 ± 0.3 | 136.0 | 304.0 |
| `Modify_6` | `-` | 119.0 ± 0.5 | 111.5 ± 0.2 | 184.0 | 496.0 |
| `Modify_loyalty` | `-` | 16.3 ± 0.0 | 16.7 ± 0.2 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.6 ± 0.0 | 7.6 ± 0.0 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 34,279.6 ± 545.8 | — | 97,424.0 | — |
| `ModifyNames` | `size=64` | 4,268.7 ± 15.0 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 574.3 ± 21.6 | — | 1,968.0 | — |
| `ModifyStreet` | `size=512` | 121.3 ± 3.6 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 118.6 ± 0.6 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 119.4 ± 1.4 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.3 ± 1.0 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.0 ± 0.4 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.3 ± 0.9 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 93,917.1 ± 329.7 | — | 382,774.1 | — |
| `monocleModifyNames` | `size=64` | 9,466.1 ± 80.8 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,329.3 ± 5.7 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 47,726.6 ± 278.6 | — | 169,079.5 | — |
| `monocleModifyStreet` | `size=64` | 6,210.8 ± 24.2 | — | 20,904.2 | — |
| `monocleModifyStreet` | `size=8` | 893.1 ± 3.1 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 26,508.1 ± 123.2 | — | 69,784.9 | — |
| `monocleReadStreet` | `size=64` | 3,430.6 ± 17.5 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 501.4 ± 2.6 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 62,267.8 ± 205.7 | — | 226,297.6 | — |
| `naiveModifyNames` | `size=64` | 7,997.9 ± 145.3 | — | 27,936.3 | — |
| `naiveModifyNames` | `size=8` | 1,058.5 ± 4.7 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 46,956.7 ± 957.8 | — | 169,057.3 | — |
| `naiveModifyStreet` | `size=64` | 6,253.9 ± 94.4 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 921.2 ± 11.3 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 26,587.4 ± 84.7 | — | 69,784.8 | — |
| `naiveReadStreet` | `size=64` | 3,446.4 ± 47.5 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 500.7 ± 3.7 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 231,659.8 ± 3,010.9 | — | 622,233.7 | — |
| `Names` | `size=64` | 28,753.8 ± 287.3 | — | 79,299.0 | — |
| `Names` | `size=8` | 3,884.1 ± 11.3 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 248,587.7 ± 2,772.9 | — | 695,855.2 | — |
| `NamesIor` | `size=64` | 30,153.3 ± 169.7 | — | 86,979.2 | — |
| `NamesIor` | `size=8` | 4,134.5 ± 110.5 | — | 11,488.1 | — |
| `Street` | `size=512` | 993.8 ± 14.5 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,005.4 ± 20.8 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,036.5 ± 64.0 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 998.3 ± 14.1 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,019.6 ± 16.7 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,008.4 ± 26.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 231,164.2 ± 4,516.7 | — | 609,881.3 | — |
| `directNames` | `size=64` | 28,166.2 ± 345.5 | — | 77,705.2 | — |
| `directNames` | `size=8` | 3,847.1 ± 50.3 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 982.6 ± 12.6 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 992.5 ± 12.1 | — | 2,744.1 | — |
| `directStreet` | `size=8` | 1,011.5 ± 10.0 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 229,699.7 ± 1,455.7 | — | 613,984.1 | — |
| `hcursorNames` | `size=64` | 27,874.2 ± 670.6 | — | 77,268.7 | — |
| `hcursorNames` | `size=8` | 3,776.8 ± 84.3 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,056.7 ± 10.6 | — | 3,032.8 | — |
| `hcursorStreet` | `size=64` | 1,061.6 ± 8.8 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,043.1 ± 13.6 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 221,430.0 ± 1,579.8 | — | 1,121,756.8 | — |
| `monocleNames` | `size=64` | 23,875.8 ± 109.5 | — | 132,764.7 | — |
| `monocleNames` | `size=8` | 4,067.1 ± 729.8 | — | 19,466.7 | — |
| `monocleStreet` | `size=512` | 176,108.1 ± 10,211.4 | — | 908,027.7 | — |
| `monocleStreet` | `size=64` | 20,454.7 ± 105.2 | — | 113,825.8 | — |
| `monocleStreet` | `size=8` | 3,064.5 ± 21.5 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 187,426.8 ± 1,820.0 | — | 965,261.9 | — |
| `naiveNames` | `size=64` | 22,324.3 ± 131.4 | — | 120,842.5 | — |
| `naiveNames` | `size=8` | 3,230.9 ± 15.3 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 168,684.8 ± 1,323.2 | — | 908,033.3 | — |
| `naiveStreet` | `size=64` | 20,664.8 ± 290.1 | — | 113,801.8 | — |
| `naiveStreet` | `size=8` | 3,068.6 ± 17.6 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 3,280.2 ± 12.1 | — | 42,026.9 | — |
| `ModifyStreet` | `size=64` | 495.8 ± 17.9 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 301.1 ± 1.9 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 209.8 ± 1.4 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 209.2 ± 0.6 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 207.5 ± 6.1 | — | 90.7 | — |
| `SumPrices` | `size=512` | 87,725.1 ± 350.1 | — | 63,712.8 | — |
| `SumPrices` | `size=64` | 11,162.8 ± 120.4 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,471.6 ± 16.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 160,825.1 ± 1,184.1 | — | 333,547.7 | — |
| `monocleModifyStreet` | `size=64` | 19,141.1 ± 93.9 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,180.5 ± 39.8 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 91,375.9 ± 1,578.5 | — | 193,230.2 | — |
| `monocleReadStreet` | `size=64` | 11,297.9 ± 233.0 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,715.4 ± 4.9 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 441,021.5 ± 4,401.6 | — | 1,178,460.7 | — |
| `monocleSumPrices` | `size=64` | 15,305.2 ± 255.8 | — | 45,841.6 | — |
| `monocleSumPrices` | `size=8` | 2,342.9 ± 38.1 | — | 6,448.0 | — |
| `naiveModifyStreet` | `size=512` | 158,992.2 ± 723.0 | — | 333,564.2 | — |
| `naiveModifyStreet` | `size=64` | 19,634.4 ± 249.6 | — | 30,058.0 | — |
| `naiveModifyStreet` | `size=8` | 3,210.6 ± 12.9 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 90,312.4 ± 1,434.7 | — | 193,229.3 | — |
| `naiveReadStreet` | `size=64` | 11,381.8 ± 323.1 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,730.7 ± 31.8 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 96,654.9 ± 1,644.7 | — | 230,122.7 | — |
| `naiveSumPrices` | `size=64` | 12,065.6 ± 240.3 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,830.0 ± 35.1 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 37,390.0 ± 1,527.6 | — | 455.3 | — |
| `nativeReadStreet` | `size=64` | 4,893.5 ± 138.2 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 882.6 ± 27.1 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 65,565.6 ± 279.6 | — | 86,272.2 | — |
| `nativeSumPrices` | `size=64` | 8,077.9 ± 37.3 | — | 10,920.9 | — |
| `nativeSumPrices` | `size=8` | 1,186.8 ± 11.0 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 123,598.8 ± 826.0 | — | 624,386.0 | — |
| `TransformDeep` | `n=512` | 13,032.0 ± 73.0 | — | 57,361.3 | — |
| `TransformDeep` | `n=64` | 1,460.6 ± 8.3 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 132,468.6 ± 1,502.1 | 172,057.5 ± 499.5 | 655,360.4 | 753,741.2 |
| `TransformExpr` | `n=512` | 16,255.6 ± 55.9 | 16,136.2 ± 26.2 | 81,825.7 | 69,585.7 |
| `TransformExpr` | `n=64` | 2,001.3 ± 7.5 | 2,632.9 ± 3.4 | 10,144.0 | 11,728.1 |
| `UniverseDeep` | `n=4096` | 117,573.7 ± 4,708.0 | — | 786,589.6 | — |
| `UniverseDeep` | `n=512` | 16,122.6 ± 25.8 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,957.1 ± 2.5 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 104,448.4 ± 1,263.9 | 3,056,262.2 ± 162,277.8 | 786,388.0 | 4,687,901.0 |
| `UniverseExpr` | `n=512` | 15,587.9 ± 35.7 | 190,778.7 ± 1,312.2 | 98,185.6 | 475,011.6 |
| `UniverseExpr` | `n=64` | 1,904.8 ± 4.4 | 17,803.1 ± 965.8 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 229,068.8 ± 4,251.6 | 3,274,747.9 ± 151,934.7 | 786,478.7 | 6,468,078.9 |
| `UniverseJson` | `n=512` | 25,613.1 ± 81.1 | 240,520.1 ± 1,488.8 | 98,186.6 | 699,920.7 |
| `UniverseJson` | `n=64` | 2,982.6 ± 7.2 | 22,055.5 ± 952.5 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 45,222.7 ± 319.6 | — | 163,888.9 | — |
| `visitorTransformDeep` | `n=512` | 4,198.9 ± 86.3 | — | 20,496.4 | — |
| `visitorTransformDeep` | `n=64` | 442.0 ± 8.5 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 66,624.2 ± 248.1 | — | 360,472.5 | — |
| `visitorTransformExpr` | `n=512` | 8,314.5 ± 23.7 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 1,014.2 ± 3.5 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 58,946.2 ± 961.8 | — | 196,706.9 | — |
| `visitorUniverseDeep` | `n=512` | 7,136.6 ± 91.2 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 793.9 ± 5.7 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 56,263.1 ± 935.3 | — | 196,657.0 | — |
| `visitorUniverseExpr` | `n=512` | 6,934.5 ± 20.6 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 811.4 ± 1.1 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 94,793.3 ± 439.5 | — | 196,701.0 | — |
| `visitorUniverseJson` | `n=512` | 9,764.5 ± 35.9 | — | 24,601.0 | — |
| `visitorUniverseJson` | `n=64` | 1,350.0 ± 3.8 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 13,324.9 ± 34.5 | — | 41,414.3 | — |
| `Modify_powerEach` | `size=16` | 262.8 ± 2.5 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,354.2 ± 41.6 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 116.8 ± 1.3 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 53,608.5 ± 1,105.6 | — | 164,375.3 | — |
| `Modify_powerEach` | `size=64` | 803.1 ± 7.8 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 58,722.6 ± 670.7 | — | 279,431.9 | — |
| `monocle_powerEach` | `size=16` | 548.7 ± 4.5 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,307.4 ± 45.8 | — | 107,331.3 | — |
| `monocle_powerEach` | `size=4` | 203.5 ± 12.9 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 180,751.4 ± 3,731.1 | — | 967,846.3 | — |
| `monocle_powerEach` | `size=64` | 2,190.0 ± 20.5 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 7,026.4 ± 23.0 | — | 28,731.3 | — |
| `naive_powerEach` | `size=16` | 108.9 ± 2.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,694.9 ± 4.5 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 26.3 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 27,834.1 ± 85.1 | — | 114,789.3 | — |
| `naive_powerEach` | `size=64` | 424.1 ± 4.3 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 73,681.4 ± 971.1 | — | 210,630.1 | — |
| `Modify_nested` | `size=16` | 1,416.4 ± 7.1 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 18,536.4 ± 100.9 | — | 53,739.7 | — |
| `Modify_nested` | `size=4` | 648.8 ± 23.6 | — | 2,184.0 | — |
| `Modify_nested` | `size=64` | 4,884.4 ± 51.1 | — | 14,693.9 | — |
| `monocle_nested` | `size=1024` | 239,044.4 ± 1,791.7 | — | 1,118,835.2 | — |
| `monocle_nested` | `size=16` | 2,361.6 ± 38.5 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 89,999.5 ± 572.6 | — | 430,208.1 | — |
| `monocle_nested` | `size=4` | 1,083.9 ± 21.7 | — | 5,557.4 | — |
| `monocle_nested` | `size=64` | 9,354.5 ± 78.1 | — | 58,833.0 | — |
| `naive_nested` | `size=1024` | 23,901.6 ± 128.9 | — | 115,075.5 | — |
| `naive_nested` | `size=16` | 391.3 ± 5.7 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,991.4 ± 30.5 | — | 29,019.7 | — |
| `naive_nested` | `size=4` | 137.6 ± 2.7 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,437.4 ± 12.8 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,351.2 ± 5.4 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 25,026.4 ± 1,009.4 | — | 104,684.9 | — |
| `Modify_sparse` | `size=32` | 356.0 ± 0.9 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 6,109.7 ± 12.9 | — | 24,785.7 | — |
| `Modify_sparse` | `size=8` | 120.9 ± 1.5 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 4,141.6 ± 30.8 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 97,590.9 ± 1,114.1 | — | 523,142.3 | — |
| `monocle_sparse` | `size=32` | 992.7 ± 23.2 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 32,524.1 ± 252.6 | — | 166,685.3 | — |
| `monocle_sparse` | `size=8` | 299.6 ± 0.7 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 316.8 ± 4.1 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 5,203.6 ± 19.5 | — | 24,612.4 | — |
| `naive_sparse` | `size=32` | 81.0 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,286.9 ± 4.0 | — | 6,176.4 | — |
| `naive_sparse` | `size=8` | 24.4 ± 0.0 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.5 ± 0.0 | 2.7 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.5 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.6 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 20.6 ± 0.2 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 35.6 ± 0.1 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.8 ± 0.0 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.3 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 93,479.0 ± 598.6 | — | 589,712.7 | — |
| `Cata` | `-` | 67,577.9 ± 355.7 | — | 197,568.5 | — |
| `Hylo` | `-` | 87,562.3 ± 552.6 | — | 295,848.6 | — |
| `drosteAna` | `-` | 45,341.1 ± 56.4 | — | 327,632.3 | — |
| `drosteCata` | `-` | 39,073.7 ± 171.0 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 49,868.1 ± 121.1 | — | 328,640.4 | — |
| `handAna` | `-` | 23,403.3 ± 355.3 | — | 163,816.2 | — |
| `handCata` | `-` | 13,312.8 ± 24.9 | — | 0.1 | — |
| `handHylo` | `-` | 10,830.8 ± 505.9 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 12.4 ± 0.3 | 27.5 ± 0.0 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.9 ± 0.2 | 58.7 ± 1.1 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,540.2 ± 93.9 | — | 20,200.9 | — |
| `FoldNested` | `size=64` | 545.8 ± 0.9 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 50.5 ± 1.0 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 2,125.9 ± 53.4 | 30,717.7 ± 149.5 | 12,312.3 | 162,564.9 |
| `FoldPrices` | `size=64` | 153.3 ± 1.4 | 2,091.0 ± 5.2 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 21.1 ± 0.0 | 276.6 ± 0.3 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 8,150.5 ± 251.4 | 33,187.8 ± 232.8 | 36,897.3 | 176,890.8 |
| `Modify` | `size=64` | 876.2 ± 1.6 | 1,827.1 ± 9.4 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 101.1 ± 0.2 | 234.0 ± 0.4 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.6 ± 0.3 | — | 0.0 | — |
| `DrillModify` | `-` | 96.1 ± 0.7 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.2 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 64.7 ± 0.3 | — | 400.0 | — |
| `handDrillGet` | `-` | 15.3 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 86.3 ± 0.5 | — | 592.0 | — |
| `handServiceGet` | `-` | 15.2 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 69.7 ± 0.6 | — | 520.0 | — |


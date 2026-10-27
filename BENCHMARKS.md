# Benchmarks

> **Generated file — do not edit.** Written by the bench-sweep
> workflow (see `.github/bench/`). eo vs [Monocle](https://www.optics.dev/Monocle/) on JMH.
>
> GitHub-hosted shared 2-vCPU runner: **B/op (allocation) is the
> authoritative, run-to-run comparable metric; ns/op is
> directional** and not comparable across runs/VMs. The usual JMH
> disclaimer applies: "the numbers below are just data".

<sub>source_sha: `0c755c831eb16fb3f9667337385926cb92d8718d` · date: `2026-10-27` · jdk: `temurin-21` · runner: `ubuntu-22.04` · jmh_params: `-i 5 -wi 3 -f 3 -t 1 -foe true -prof gc -rf json -jvmArgsAppend -XX:-ResizeTLAB -jvmArgsAppend -Xms1g -jvmArgsAppend -Xmx1g` · profile: `sweep:-i5-wi3-f3-t1-gc-rt1g`</sub>


## AffineFoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOption_0` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_0_asAffineFold` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `GetOption_0_asOptional` | `-` | 2.2 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.7 ± 0.4 | 9.7 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 28.7 ± 0.3 | 23.8 ± 0.1 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 159.9 ± 3.2 | — | 720.0 | — |
| `ModifyCountry` | `-` | 313.2 ± 11.3 | — | 928.0 | — |
| `ModifyPartner` | `-` | 397.6 ± 8.9 | — | 984.0 | — |
| `ReadCountry` | `-` | 177.8 ± 4.7 | — | 520.0 | — |
| `ReadPartner` | `-` | 207.9 ± 2.5 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 315.8 ± 8.0 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,634.5 ± 19.5 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 2,934.0 ± 67.7 | — | 8,696.0 | — |
| `naivePassthroughPayload` | `-` | 4,853.6 ± 34.9 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,677.9 ± 47.3 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 2,082.1 ± 44.2 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 932.0 ± 6.6 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 628.1 ± 10.2 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 404.0 ± 3.7 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 427.2 ± 13.4 | — | 1,565.3 | — |
| `confluentRecordReaderFresh` | `-` | 1,357.4 ± 23.2 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,339.9 ± 13.6 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 54.1 ± 0.9 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 623.0 ± 15.5 | — | 568.0 | — |
| `handwritten_stream` | `-` | 158.2 ± 1.1 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 656.6 ± 10.4 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,925.0 ± 15.8 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,781.5 ± 28.6 | — | 3,968.0 | — |
| `WideToAvro` | `-` | 855.3 ± 26.5 | — | 2,029.3 | — |
| `WideToJson` | `-` | 687.2 ± 13.9 | — | 1,472.0 | — |
| `naiveClickToAvro` | `-` | 1,472.0 ± 17.6 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,926.0 ± 42.0 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 968.2 ± 16.6 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,883.9 ± 11.8 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 199.0 ± 15.3 | — | 832.0 | — |
| `decode_native` | `-` | 17.3 ± 0.1 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 187.4 ± 0.6 | — | 832.0 | — |
| `encode_bridged` | `-` | 250.9 ± 8.2 | — | 1,240.0 | — |
| `encode_native` | `-` | 11.7 ± 0.0 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 260.3 ± 4.9 | — | 1,245.3 | — |
| `fieldGet_bridged` | `-` | 100.1 ± 1.1 | — | 432.0 | — |
| `fieldGet_native` | `-` | 101.1 ± 1.7 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 395.3 ± 3.4 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 167.2 ± 3.9 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.5 ± 0.2 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.2 ± 0.1 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.1 ± 0.2 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.6 ± 0.2 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.8 ± 0.2 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.1 ± 0.2 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.2 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.5 ± 0.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 24.3 ± 0.1 | — | 160.0 | — |
| `GetterRead` | `-` | 13.5 ± 0.3 | — | 48.0 | — |
| `Patch` | `-` | 6.4 ± 0.1 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 16.1 ± 0.3 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 21.1 ± 0.2 | — | 160.0 | — |
| `TransformFocus` | `-` | 832.7 ± 8.8 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,191.6 ± 86.9 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 914.1 ± 21.3 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.4 ± 0.1 | — | 48.0 | — |
| `TransformImage` | `-` | 22.6 ± 0.1 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 22.9 ± 0.1 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.3 ± 0.2 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.9 ± 0.2 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.6 ± 0.1 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.5 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.9 ± 0.1 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.3 ± 0.1 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 809.8 ± 6.2 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 976.4 ± 28.6 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,339.2 ± 10.9 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.4 ± 0.1 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.7 ± 0.2 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.4 ± 0.1 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.8 ± 0.0 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.3 ± 0.0 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.4 ± 0.0 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 414.5 ± 0.7 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 396.9 ± 2.6 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 270.8 ± 1.9 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.5 ± 0.0 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.8 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 519.3 ± 11.5 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 527.9 ± 12.3 | — | 744.0 | — |
| `encode_hand` | `-` | 142.0 ± 2.2 | — | 768.0 | — |
| `encode_positional` | `-` | 6,369.9 ± 28.4 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,778.3 ± 45.7 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.8 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 39.1 ± 0.5 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 26.2 ± 0.2 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.4 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.3 ± 0.1 | — | 72.0 | — |
| `reuseLens6` | `-` | 123.8 ± 0.6 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.1 ± 0.2 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,241.7 ± 7.9 | 1,245.0 ± 7.6 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 157.9 ± 1.2 | 158.3 ± 2.5 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.4 ± 0.0 | 21.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,153.2 ± 33.7 | 2,147.4 ± 56.0 | 12,312.4 | 12,312.3 |
| `FoldPrices` | `size=64` | 158.1 ± 1.8 | 157.9 ± 0.9 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.9 ± 0.2 | 26.4 ± 0.1 | 216.0 | 216.0 |

## GenericsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `genLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `genLensModify` | `-` | 3.6 ± 0.0 | — | 24.0 | — |
| `genPrismGetHit` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `genPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `genPrismModifyHit` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `genPrismModifyMiss` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `handLensGet` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismGetHit` | `-` | 2.0 ± 0.0 | — | 16.0 | — |
| `handPrismGetMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `handPrismModifyHit` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
| `handPrismModifyMiss` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `rawLensGet` | `-` | 0.6 ± 0.0 | — | 0.0 | — |
| `rawLensModify` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `rawPrismGetHit` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `rawPrismModifyHit` | `-` | 2.3 ± 0.1 | — | 24.0 | — |

## GetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get_0` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |
| `Get_3` | `-` | 16.3 ± 0.1 | 9.5 ± 0.4 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.6 ± 0.1 | 24.8 ± 0.3 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.7 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.1 ± 0.0 | 3.2 ± 0.1 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 423,428.5 ± 13,857.2 | — | 1,066,773.0 | — |
| `cModifyId` | `size=64` | 51,446.2 ± 520.8 | — | 136,293.5 | — |
| `cModifyId` | `size=8` | 8,420.3 ± 56.0 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 209,138.0 ± 4,377.8 | — | 797,931.5 | — |
| `cReadId` | `size=64` | 25,933.9 ± 230.4 | — | 101,294.2 | — |
| `cReadId` | `size=8` | 4,082.7 ± 115.4 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 205,537.4 ± 4,332.5 | — | 797,930.4 | — |
| `cReadStreet` | `size=64` | 25,918.3 ± 339.5 | — | 101,294.1 | — |
| `cReadStreet` | `size=8` | 4,062.4 ± 27.7 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 408,456.7 ± 8,638.2 | — | 1,066,706.1 | — |
| `cReplaceId` | `size=64` | 52,002.6 ± 1,200.7 | — | 136,245.5 | — |
| `cReplaceId` | `size=8` | 8,515.3 ± 63.4 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 338,954.7 ± 3,258.5 | — | 1,240,739.0 | — |
| `cSumPrices` | `size=64` | 42,026.0 ± 1,198.1 | — | 157,039.8 | — |
| `cSumPrices` | `size=8` | 6,120.3 ± 41.8 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 180.0 ± 3.1 | — | 0.1 | — |
| `jMiss` | `size=64` | 175.8 ± 1.7 | — | 0.0 | — |
| `jMiss` | `size=8` | 180.1 ± 3.6 | — | 0.0 | — |
| `jModifyId` | `size=512` | 4,098.8 ± 32.4 | — | 41,929.2 | — |
| `jModifyId` | `size=64` | 529.1 ± 9.5 | — | 5,336.0 | — |
| `jModifyId` | `size=8` | 119.3 ± 2.8 | — | 976.0 | — |
| `jReadId` | `size=512` | 34.4 ± 0.4 | — | 48.0 | — |
| `jReadId` | `size=64` | 35.9 ± 2.6 | — | 56.0 | — |
| `jReadId` | `size=8` | 35.0 ± 0.9 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 194.8 ± 3.5 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 192.2 ± 3.1 | — | 136.0 | — |
| `jReadStreet` | `size=8` | 194.2 ± 2.9 | — | 136.0 | — |
| `jReplaceId` | `size=512` | 4,139.3 ± 86.5 | — | 41,889.2 | — |
| `jReplaceId` | `size=64` | 529.6 ± 15.9 | — | 5,304.0 | — |
| `jReplaceId` | `size=8` | 121.6 ± 7.0 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 89,946.0 ± 3,942.4 | — | 63,659.3 | — |
| `jSumPrices` | `size=64` | 12,245.3 ± 1,080.8 | — | 8,120.4 | — |
| `jSumPrices` | `size=8` | 1,523.5 ± 112.3 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 87.5 ± 2.0 | — | 320.0 | — |
| `MapDrillModify` | `-` | 39.0 ± 0.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 64.4 ± 0.8 | — | 216.0 | — |
| `handEnvUse` | `-` | 88.6 ± 2.7 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.1 ± 0.1 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 61.3 ± 0.7 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.1 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.2 ± 0.0 | 4.8 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 33.4 ± 0.3 | 29.9 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.9 ± 0.1 | 3.8 ± 0.1 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,406.7 ± 641.6 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,226.3 ± 69.3 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 375.9 ± 1.8 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 84.8 ± 0.6 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 51,796.9 ± 2,381.0 | — | 331,117.3 | — |
| `Modify_multiFocus` | `size=256` | 11,431.8 ± 74.3 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,382.8 ± 17.3 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 222.1 ± 3.6 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 35,005.8 ± 552.9 | — | 115,177.1 | — |
| `Modify_powerEach` | `size=256` | 8,234.4 ± 68.7 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 999.8 ± 5.1 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 204.3 ± 12.4 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,576.0 ± 65.3 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,110.0 ± 28.3 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 253.9 ± 1.7 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 33.8 ± 0.5 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,130.5 ± 42.7 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 728.7 ± 44.9 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.3 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.1 ± 0.2 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.6 ± 0.6 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 124.9 ± 0.7 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 9.7 ± 0.4 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 31.2 ± 0.5 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.6 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 8.9 ± 0.2 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 16.6 ± 0.3 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 160.9 ± 0.9 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.6 ± 0.2 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,126.0 ± 12.5 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,091.3 ± 37.9 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.2 ± 0.2 | 21.7 ± 0.1 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 45.0 ± 0.1 | 71.3 ± 0.8 | 136.0 | 304.0 |
| `Modify_6` | `-` | 111.8 ± 5.9 | 119.9 ± 0.7 | 184.0 | 496.0 |
| `Modify_loyalty` | `-` | 15.2 ± 0.0 | 16.3 ± 0.1 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.7 ± 0.1 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 8.9 ± 0.2 | 7.2 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 32,165.7 ± 1,214.1 | — | 97,423.5 | — |
| `ModifyNames` | `size=64` | 3,990.8 ± 56.7 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 539.6 ± 7.3 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 112.2 ± 1.5 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 110.5 ± 0.5 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 111.5 ± 1.6 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.1 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.3 ± 0.5 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.2 ± 0.8 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 91,957.2 ± 898.5 | — | 382,774.9 | — |
| `monocleModifyNames` | `size=64` | 9,057.1 ± 119.8 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,415.4 ± 22.0 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 45,739.4 ± 244.6 | — | 169,078.9 | — |
| `monocleModifyStreet` | `size=64` | 6,013.2 ± 52.3 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 925.0 ± 13.0 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 24,986.1 ± 133.3 | — | 69,783.6 | — |
| `monocleReadStreet` | `size=64` | 3,215.9 ± 14.6 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 475.9 ± 3.4 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 59,460.9 ± 300.3 | — | 226,297.1 | — |
| `naiveModifyNames` | `size=64` | 7,553.0 ± 203.0 | — | 27,937.1 | — |
| `naiveModifyNames` | `size=8` | 1,088.5 ± 4.4 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 45,835.2 ± 389.1 | — | 169,056.7 | — |
| `naiveModifyStreet` | `size=64` | 6,005.2 ± 22.9 | — | 20,880.2 | — |
| `naiveModifyStreet` | `size=8` | 925.4 ± 6.8 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,160.0 ± 153.6 | — | 69,783.4 | — |
| `naiveReadStreet` | `size=64` | 3,226.4 ± 20.3 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 474.7 ± 3.1 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 253,572.0 ± 3,851.3 | — | 622,251.2 | — |
| `Names` | `size=64` | 31,042.8 ± 525.1 | — | 79,299.3 | — |
| `Names` | `size=8` | 4,222.3 ± 138.4 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 261,262.9 ± 5,131.3 | — | 687,657.4 | — |
| `NamesIor` | `size=64` | 33,352.7 ± 409.5 | — | 87,497.7 | — |
| `NamesIor` | `size=8` | 4,421.3 ± 37.9 | — | 11,488.1 | — |
| `Street` | `size=512` | 1,069.0 ± 40.0 | — | 2,720.8 | — |
| `Street` | `size=64` | 1,064.2 ± 30.2 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,160.6 ± 116.2 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,075.6 ± 12.6 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,077.5 ± 6.9 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,066.3 ± 14.6 | — | 2,736.0 | — |
| `directNames` | `size=512` | 240,682.2 ± 983.3 | — | 609,888.9 | — |
| `directNames` | `size=64` | 30,820.8 ± 375.0 | — | 76,668.0 | — |
| `directNames` | `size=8` | 4,105.2 ± 57.8 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,077.8 ± 32.6 | — | 2,744.9 | — |
| `directStreet` | `size=64` | 1,062.5 ± 35.7 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,061.1 ± 21.2 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 245,281.4 ± 3,875.0 | — | 613,995.4 | — |
| `hcursorNames` | `size=64` | 30,351.1 ± 921.9 | — | 77,273.1 | — |
| `hcursorNames` | `size=8` | 4,049.2 ± 65.2 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,134.6 ± 8.2 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,125.5 ± 17.4 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,109.7 ± 14.9 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 224,872.8 ± 2,488.6 | — | 1,121,764.4 | — |
| `monocleNames` | `size=64` | 24,224.3 ± 102.6 | — | 132,770.7 | — |
| `monocleNames` | `size=8` | 3,673.3 ± 18.0 | — | 19,461.4 | — |
| `monocleStreet` | `size=512` | 183,215.2 ± 4,483.9 | — | 908,027.1 | — |
| `monocleStreet` | `size=64` | 21,115.4 ± 379.9 | — | 113,449.9 | — |
| `monocleStreet` | `size=8` | 3,185.3 ± 12.4 | — | 17,048.1 | — |
| `naiveNames` | `size=512` | 198,874.2 ± 7,404.3 | — | 965,269.6 | — |
| `naiveNames` | `size=64` | 22,999.1 ± 249.4 | — | 120,847.8 | — |
| `naiveNames` | `size=8` | 3,366.1 ± 31.1 | — | 17,808.1 | — |
| `naiveStreet` | `size=512` | 184,588.2 ± 5,885.4 | — | 908,033.4 | — |
| `naiveStreet` | `size=64` | 21,268.9 ± 184.8 | — | 113,812.6 | — |
| `naiveStreet` | `size=8` | 3,193.0 ± 23.0 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,117.0 ± 59.8 | — | 42,027.6 | — |
| `ModifyStreet` | `size=64` | 681.2 ± 5.3 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 299.1 ± 2.5 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 197.0 ± 4.6 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 196.5 ± 3.3 | — | 109.4 | — |
| `ReadStreet` | `size=8` | 196.7 ± 1.5 | — | 128.0 | — |
| `SumPrices` | `size=512` | 84,633.0 ± 1,584.7 | — | 63,713.5 | — |
| `SumPrices` | `size=64` | 10,985.0 ± 377.2 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,578.0 ± 81.1 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 159,936.8 ± 2,962.3 | — | 333,573.0 | — |
| `monocleModifyStreet` | `size=64` | 19,143.9 ± 74.2 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,289.0 ± 173.5 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 88,282.1 ± 724.5 | — | 193,227.6 | — |
| `monocleReadStreet` | `size=64` | 11,136.3 ± 55.4 | — | 24,705.1 | — |
| `monocleReadStreet` | `size=8` | 1,789.9 ± 14.1 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 418,802.2 ± 4,398.7 | — | 1,178,439.5 | — |
| `monocleSumPrices` | `size=64` | 15,426.5 ± 120.7 | — | 45,833.6 | — |
| `monocleSumPrices` | `size=8` | 2,426.0 ± 40.6 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 157,181.2 ± 712.3 | — | 333,562.7 | — |
| `naiveModifyStreet` | `size=64` | 18,953.8 ± 304.7 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,191.9 ± 49.7 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 88,820.0 ± 550.4 | — | 193,228.0 | — |
| `naiveReadStreet` | `size=64` | 11,456.7 ± 61.6 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,824.5 ± 201.3 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 92,702.8 ± 999.5 | — | 230,119.4 | — |
| `naiveSumPrices` | `size=64` | 12,092.6 ± 46.9 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,892.1 ± 28.1 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,106.6 ± 365.1 | — | 451.7 | — |
| `nativeReadStreet` | `size=64` | 4,269.3 ± 74.8 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 806.6 ± 7.7 | — | 424.0 | — |
| `nativeSumPrices` | `size=512` | 62,322.3 ± 1,140.5 | — | 86,271.6 | — |
| `nativeSumPrices` | `size=64` | 7,692.6 ± 76.9 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,141.2 ± 18.9 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 118,898.4 ± 491.0 | — | 624,382.5 | — |
| `TransformDeep` | `n=512` | 11,724.1 ± 99.8 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,423.7 ± 3.9 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 129,619.4 ± 993.0 | 163,118.8 ± 809.7 | 655,358.3 | 753,734.7 |
| `TransformExpr` | `n=512` | 15,928.4 ± 71.9 | 15,392.7 ± 142.3 | 81,825.6 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,997.1 ± 10.4 | 2,482.0 ± 21.6 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 98,043.9 ± 1,330.2 | — | 786,575.4 | — |
| `UniverseDeep` | `n=512` | 15,260.2 ± 68.0 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,867.2 ± 14.5 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 99,493.8 ± 2,932.1 | 2,902,103.1 ± 181,505.2 | 786,384.4 | 4,687,789.0 |
| `UniverseExpr` | `n=512` | 14,805.7 ± 91.4 | 209,070.6 ± 931.3 | 98,185.5 | 475,013.5 |
| `UniverseExpr` | `n=64` | 1,797.3 ± 9.4 | 17,849.7 ± 723.3 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 216,689.1 ± 1,871.9 | 3,211,468.9 ± 124,488.9 | 786,469.7 | 6,468,032.9 |
| `UniverseJson` | `n=512` | 24,656.1 ± 81.3 | 249,278.4 ± 3,826.4 | 98,186.5 | 699,921.5 |
| `UniverseJson` | `n=64` | 2,976.7 ± 6.3 | 23,060.1 ± 649.4 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 41,640.8 ± 392.4 | — | 163,886.3 | — |
| `visitorTransformDeep` | `n=512` | 4,916.4 ± 87.4 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 426.9 ± 1.5 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 63,963.5 ± 570.6 | — | 360,470.6 | — |
| `visitorTransformExpr` | `n=512` | 7,531.5 ± 42.6 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 981.3 ± 1.2 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,820.5 ± 1,047.7 | — | 196,703.9 | — |
| `visitorUniverseDeep` | `n=512` | 6,497.9 ± 86.2 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 748.4 ± 5.0 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,821.6 ± 471.3 | — | 196,654.4 | — |
| `visitorUniverseExpr` | `n=512` | 6,517.8 ± 24.1 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 799.1 ± 5.6 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 103,354.2 ± 1,681.7 | — | 196,707.2 | — |
| `visitorUniverseJson` | `n=512` | 9,199.9 ± 41.1 | — | 24,600.9 | — |
| `visitorUniverseJson` | `n=64` | 1,348.5 ± 11.6 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,169.6 ± 39.9 | — | 41,413.7 | — |
| `Modify_powerEach` | `size=16` | 258.4 ± 2.0 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 3,022.1 ± 26.7 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 123.0 ± 0.5 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,602.6 ± 2,209.4 | — | 164,372.0 | — |
| `Modify_powerEach` | `size=64` | 789.4 ± 9.9 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 60,686.8 ± 691.2 | — | 279,433.4 | — |
| `monocle_powerEach` | `size=16` | 605.4 ± 3.7 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 22,096.8 ± 886.8 | — | 107,331.5 | — |
| `monocle_powerEach` | `size=4` | 210.2 ± 2.0 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 177,777.9 ± 1,441.3 | — | 967,841.5 | — |
| `monocle_powerEach` | `size=64` | 2,157.7 ± 5.6 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 6,730.8 ± 33.8 | — | 28,731.1 | — |
| `naive_powerEach` | `size=16` | 101.0 ± 0.2 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,618.5 ± 9.2 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.1 ± 0.1 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,925.8 ± 176.5 | — | 114,788.0 | — |
| `naive_powerEach` | `size=64` | 396.2 ± 1.8 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 73,638.9 ± 1,764.1 | — | 210,502.0 | — |
| `Modify_nested` | `size=16` | 1,446.6 ± 23.4 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,689.9 ± 217.4 | — | 53,699.1 | — |
| `Modify_nested` | `size=4` | 682.1 ± 7.9 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,562.1 ± 28.6 | — | 14,640.5 | — |
| `monocle_nested` | `size=1024` | 249,601.4 ± 1,637.3 | — | 1,118,854.4 | — |
| `monocle_nested` | `size=16` | 2,611.8 ± 15.0 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 91,865.4 ± 508.2 | — | 430,208.7 | — |
| `monocle_nested` | `size=4` | 1,286.2 ± 90.5 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,935.4 ± 245.4 | — | 58,860.4 | — |
| `naive_nested` | `size=1024` | 21,728.7 ± 165.5 | — | 115,071.6 | — |
| `naive_nested` | `size=16` | 364.4 ± 2.6 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,231.3 ± 27.3 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 125.6 ± 2.1 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,325.9 ± 12.4 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,327.5 ± 12.6 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,121.6 ± 293.4 | — | 104,675.1 | — |
| `Modify_sparse` | `size=32` | 352.3 ± 13.0 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,677.6 ± 22.7 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 119.1 ± 0.3 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,868.9 ± 29.5 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 108,355.9 ± 2,286.3 | — | 523,147.6 | — |
| `monocle_sparse` | `size=32` | 1,066.0 ± 26.8 | — | 7,032.0 | — |
| `monocle_sparse` | `size=512` | 33,535.8 ± 391.0 | — | 166,685.9 | — |
| `monocle_sparse` | `size=8` | 343.3 ± 6.1 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 286.9 ± 1.0 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,979.8 ± 65.4 | — | 24,612.3 | — |
| `naive_sparse` | `size=32` | 74.0 ± 0.2 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,167.2 ± 3.8 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 24.9 ± 0.1 | — | 128.0 | — |

## PrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GetOptionAbsent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOptionPresent` | `-` | 0.9 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetRightAbsent` | `-` | 1.1 ± 0.0 | 1.1 ± 0.0 | 0.0 | 0.0 |
| `GetRightPresent` | `-` | 2.3 ± 0.0 | 2.4 ± 0.0 | 16.0 | 16.0 |
| `ReverseGet` | `-` | 2.3 ± 0.0 | 2.6 ± 0.0 | 16.0 | 16.0 |
| `RightReverseGet` | `-` | 2.4 ± 0.1 | 2.6 ± 0.0 | 16.0 | 16.0 |

## ReviewBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ReverseGet_0` | `-` | 2.4 ± 0.0 | — | 24.0 | — |
| `ReverseGet_3` | `-` | 19.3 ± 0.0 | — | 72.0 | — |
| `ReverseGet_6` | `-` | 34.0 ± 0.2 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.7 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.1 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 96,090.6 ± 1,070.8 | — | 589,712.7 | — |
| `Cata` | `-` | 69,925.5 ± 1,042.5 | — | 197,568.5 | — |
| `Hylo` | `-` | 90,972.4 ± 240.7 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,250.3 ± 326.9 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,020.8 ± 180.7 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 64,051.0 ± 776.7 | — | 328,640.5 | — |
| `handAna` | `-` | 20,436.4 ± 57.6 | — | 163,816.1 | — |
| `handCata` | `-` | 12,621.7 ± 68.8 | — | 0.1 | — |
| `handHylo` | `-` | 9,973.9 ± 652.3 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.2 ± 0.1 | 28.1 ± 0.1 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.7 ± 0.5 | 64.9 ± 0.9 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.4 ± 0.1 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 5,035.0 ± 85.8 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 535.5 ± 4.4 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 51.2 ± 4.1 | — | 360.0 | — |
| `FoldPrices` | `size=512` | 1,889.1 ± 49.1 | 31,630.7 ± 770.6 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 156.2 ± 0.6 | 2,059.6 ± 11.5 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.6 ± 0.1 | 329.5 ± 5.0 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,335.5 ± 149.8 | 33,470.1 ± 271.5 | 36,897.2 | 176,891.0 |
| `Modify` | `size=64` | 840.1 ± 1.9 | 1,782.8 ± 7.3 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 94.9 ± 0.2 | 283.5 ± 1.4 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.3 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 94.8 ± 0.9 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.3 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 65.5 ± 0.4 | — | 400.0 | — |
| `handDrillGet` | `-` | 13.9 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 91.3 ± 0.5 | — | 592.0 | — |
| `handServiceGet` | `-` | 13.9 ± 0.0 | — | 120.0 | — |
| `handServiceReplace` | `-` | 74.5 ± 1.0 | — | 520.0 | — |


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
| `GetOption_0_asOptional` | `-` | 2.1 ± 0.0 | — | 16.0 | — |
| `GetOption_0_empty` | `-` | 0.9 ± 0.0 | 0.9 ± 0.0 | 0.0 | 0.0 |
| `GetOption_3` | `-` | 14.9 ± 0.6 | 9.8 ± 0.0 | 16.0 | 0.0 |
| `GetOption_6` | `-` | 28.6 ± 0.2 | 23.9 ± 0.3 | 16.0 | 0.0 |
| `GetOption_loyalty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |
| `GetOption_loyalty_empty` | `-` | 1.0 ± 0.0 | 1.0 ± 0.0 | 0.0 | 0.0 |

## AvroBytesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `GraftPayload` | `-` | 158.4 ± 0.8 | — | 720.0 | — |
| `ModifyCountry` | `-` | 319.6 ± 12.5 | — | 928.0 | — |
| `ModifyPartner` | `-` | 384.5 ± 22.3 | — | 989.3 | — |
| `ReadCountry` | `-` | 176.7 ± 6.5 | — | 520.0 | — |
| `ReadPartner` | `-` | 209.3 ± 2.6 | — | 480.0 | — |
| `SliceGraftPayload` | `-` | 306.6 ± 1.1 | — | 1,192.0 | — |
| `naiveModifyCountry` | `-` | 2,627.9 ± 22.4 | — | 7,616.0 | — |
| `naiveModifyPartner` | `-` | 3,019.7 ± 183.8 | — | 8,696.1 | — |
| `naivePassthroughPayload` | `-` | 4,850.7 ± 39.7 | — | 14,088.1 | — |
| `naiveReadCountry` | `-` | 1,880.6 ± 465.0 | — | 4,256.0 | — |
| `naiveReadPartner` | `-` | 1,999.9 ± 12.2 | — | 5,424.0 | — |
| `prunedReadCountry` | `-` | 950.3 ± 23.1 | — | 1,976.0 | — |
| `prunedReadPartner` | `-` | 626.0 ± 9.5 | — | 1,592.0 | — |

## AvroDecodeReuseBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cachedDecodeRecord` | `-` | 402.7 ± 1.2 | — | 1,224.0 | — |
| `confluentRecordReader` | `-` | 438.0 ± 2.7 | — | 1,560.0 | — |
| `confluentRecordReaderFresh` | `-` | 1,382.5 ± 19.4 | — | 3,696.0 | — |
| `freshDecodeRecord` | `-` | 1,359.1 ± 16.8 | — | 3,344.0 | — |

## AvroEncodeRouteBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `eo_encodeToAny` | `-` | 53.9 ± 0.1 | — | 312.0 | — |
| `eo_encodeValue` | `-` | 623.9 ± 20.0 | — | 568.0 | — |
| `handwritten_stream` | `-` | 159.1 ± 0.9 | — | 240.0 | — |
| `naive_freshPlumbing` | `-` | 654.6 ± 8.4 | — | 2,624.0 | — |

## AvroJsonBridgeBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ClickToAvro` | `-` | 2,951.0 ± 32.0 | — | 4,664.0 | — |
| `ClickToJson` | `-` | 2,784.9 ± 15.8 | — | 3,968.0 | — |
| `WideToAvro` | `-` | 874.1 ± 21.7 | — | 2,040.0 | — |
| `WideToJson` | `-` | 675.2 ± 24.7 | — | 1,461.3 | — |
| `naiveClickToAvro` | `-` | 1,453.8 ± 11.4 | — | 3,928.0 | — |
| `naiveClickToJson` | `-` | 2,912.6 ± 38.1 | — | 5,856.0 | — |
| `naiveWideToAvro` | `-` | 973.8 ± 8.1 | — | 3,504.0 | — |
| `naiveWideToJson` | `-` | 1,854.2 ± 31.2 | — | 4,376.0 | — |

## AvroVulcanBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `decode_bridged` | `-` | 190.6 ± 1.2 | — | 832.0 | — |
| `decode_native` | `-` | 17.5 ± 0.6 | — | 48.0 | — |
| `decode_vulcanRaw` | `-` | 186.7 ± 0.7 | — | 832.0 | — |
| `encode_bridged` | `-` | 250.6 ± 8.3 | — | 1,234.7 | — |
| `encode_native` | `-` | 12.2 ± 0.5 | — | 56.0 | — |
| `encode_vulcanRaw` | `-` | 248.6 ± 7.1 | — | 1,240.0 | — |
| `fieldGet_bridged` | `-` | 99.7 ± 0.7 | — | 432.0 | — |
| `fieldGet_native` | `-` | 100.5 ± 0.7 | — | 432.0 | — |
| `rootGet_bridged` | `-` | 396.9 ± 6.1 | — | 1,424.0 | — |
| `rootGet_native` | `-` | 174.2 ± 8.8 | — | 600.0 | — |

## CapsBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `foldMapCap` | `-` | 21.5 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedHeld` | `-` | 22.1 ± 0.0 | — | 0.0 | — |
| `foldMapDerivedPerCall` | `-` | 21.1 ± 0.1 | — | 0.0 | — |
| `foldMapDirect` | `-` | 21.5 ± 0.1 | — | 0.0 | — |
| `getCap` | `-` | 1.2 ± 0.0 | — | 0.0 | — |
| `getDeepCap` | `-` | 1.5 ± 0.0 | — | 0.0 | — |
| `getDeepDirect` | `-` | 1.4 ± 0.0 | — | 0.0 | — |
| `getDerivedHeld` | `-` | 2.3 ± 0.0 | — | 0.0 | — |
| `getDerivedPerCall` | `-` | 1.6 ± 0.0 | — | 0.0 | — |
| `getDirect` | `-` | 1.0 ± 0.0 | — | 0.0 | — |
| `modifyCap` | `-` | 4.2 ± 0.0 | — | 40.0 | — |
| `modifyDeepCap` | `-` | 29.7 ± 0.1 | — | 176.0 | — |
| `modifyDeepDirect` | `-` | 33.0 ± 0.0 | — | 152.0 | — |
| `modifyDerivedHeld` | `-` | 5.2 ± 0.1 | — | 40.0 | — |
| `modifyDerivedPerCall` | `-` | 4.7 ± 0.0 | — | 40.0 | — |
| `modifyDirect` | `-` | 4.1 ± 0.0 | — | 40.0 | — |

## ChimneyBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CodecModify` | `-` | 30.3 ± 0.2 | — | 152.0 | — |
| `CodecModifyMiss` | `-` | 24.6 ± 0.3 | — | 160.0 | — |
| `GetterRead` | `-` | 13.5 ± 0.4 | — | 48.0 | — |
| `Patch` | `-` | 6.5 ± 0.3 | — | 48.0 | — |
| `ResultFoldRead` | `-` | 15.8 ± 0.3 | — | 96.0 | — |
| `ResultFoldReadMiss` | `-` | 21.1 ± 0.4 | — | 160.0 | — |
| `TransformFocus` | `-` | 833.1 ± 9.5 | — | 5,464.0 | — |
| `TransformFocusPartial` | `-` | 1,133.7 ± 19.3 | — | 6,504.0 | — |
| `TransformFocusPartialFailing` | `-` | 898.8 ± 3.9 | — | 7,288.0 | — |
| `TransformFocusWrite` | `-` | 8.3 ± 0.0 | — | 48.0 | — |
| `TransformImage` | `-` | 22.8 ± 0.5 | — | 96.0 | — |
| `TransformImagePartial` | `-` | 23.0 ± 0.3 | — | 120.0 | — |
| `TransformImagePartialFailing` | `-` | 36.5 ± 0.2 | — | 184.0 | — |
| `chimneyCodecModify` | `-` | 27.6 ± 0.3 | — | 128.0 | — |
| `chimneyCodecModifyMiss` | `-` | 14.6 ± 0.0 | — | 72.0 | — |
| `chimneyGetterRead` | `-` | 1.1 ± 0.0 | — | 0.0 | — |
| `chimneyPatch` | `-` | 4.5 ± 0.0 | — | 48.0 | — |
| `chimneyResultFoldRead` | `-` | 13.9 ± 0.0 | — | 72.0 | — |
| `chimneyResultFoldReadMiss` | `-` | 19.3 ± 0.1 | — | 160.0 | — |
| `chimneyTransformFocus` | `-` | 817.9 ± 14.5 | — | 6,144.0 | — |
| `chimneyTransformFocusPartial` | `-` | 957.6 ± 3.2 | — | 6,256.0 | — |
| `chimneyTransformFocusPartialFailing` | `-` | 1,341.6 ± 8.0 | — | 10,616.0 | — |
| `chimneyTransformFocusWrite` | `-` | 4.3 ± 0.0 | — | 48.0 | — |
| `chimneyTransformImage` | `-` | 28.6 ± 0.1 | — | 152.0 | — |
| `chimneyTransformImagePartial` | `-` | 15.5 ± 0.3 | — | 96.0 | — |
| `chimneyTransformImagePartialFailing` | `-` | 18.8 ± 0.1 | — | 144.0 | — |
| `handCodecModify` | `-` | 14.3 ± 0.0 | — | 72.0 | — |
| `handCodecModifyMiss` | `-` | 0.9 ± 0.0 | — | 0.0 | — |
| `handGetterRead` | `-` | 0.8 ± 0.0 | — | 0.0 | — |
| `handPatch` | `-` | 4.3 ± 0.1 | — | 48.0 | — |
| `handResultFoldRead` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handResultFoldReadMiss` | `-` | 1.9 ± 0.0 | — | 16.0 | — |
| `handTransformFocus` | `-` | 413.9 ± 2.6 | — | 3,072.0 | — |
| `handTransformFocusPartial` | `-` | 396.2 ± 8.5 | — | 4,624.0 | — |
| `handTransformFocusPartialFailing` | `-` | 271.3 ± 2.3 | — | 3,088.0 | — |
| `handTransformFocusWrite` | `-` | 4.8 ± 0.0 | — | 48.0 | — |
| `handTransformImage` | `-` | 18.4 ± 0.0 | — | 96.0 | — |
| `handTransformImagePartial` | `-` | 5.7 ± 0.1 | — | 64.0 | — |
| `handTransformImagePartialFailing` | `-` | 0.9 ± 0.0 | — | 0.0 | — |

## ClickRecordBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `encode_derived` | `-` | 522.3 ± 6.8 | — | 744.0 | — |
| `encode_derivedThroughPrism` | `-` | 533.5 ± 13.4 | — | 744.0 | — |
| `encode_hand` | `-` | 141.0 ± 2.9 | — | 768.0 | — |
| `encode_positional` | `-` | 6,349.9 ± 45.8 | — | 23,912.0 | — |
| `encode_vulcanFull` | `-` | 7,802.6 ± 78.8 | — | 28,408.1 | — |

## CompositionBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `buildLens1` | `-` | 5.7 ± 0.1 | — | 72.0 | — |
| `buildLens3` | `-` | 21.5 ± 0.1 | — | 184.0 | — |
| `buildLens6` | `-` | 38.8 ± 0.6 | — | 352.0 | — |
| `buildLensOptional3` | `-` | 25.9 ± 0.2 | — | 240.0 | — |
| `reuseLeaf` | `-` | 3.0 ± 0.0 | — | 24.0 | — |
| `reuseLens1` | `-` | 16.5 ± 0.2 | — | 40.0 | — |
| `reuseLens3` | `-` | 45.6 ± 0.7 | — | 72.0 | — |
| `reuseLens6` | `-` | 124.0 ± 2.5 | — | 120.0 | — |
| `reuseLensOptional3` | `-` | 45.1 ± 0.2 | — | 136.0 | — |

## FoldBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldMap` | `size=512` | 1,237.9 ± 8.7 | 1,242.0 ± 8.7 | 7,936.2 | 7,936.2 |
| `FoldMap` | `size=64` | 157.4 ± 0.6 | 157.4 ± 0.7 | 768.0 | 768.0 |
| `FoldMap` | `size=8` | 21.5 ± 0.1 | 21.0 ± 0.1 | 0.0 | 0.0 |
| `FoldPrices` | `size=512` | 2,168.3 ± 33.6 | 2,151.1 ± 24.2 | 12,312.4 | 12,312.4 |
| `FoldPrices` | `size=64` | 156.3 ± 0.9 | 157.4 ± 0.6 | 1,560.0 | 1,560.0 |
| `FoldPrices` | `size=8` | 25.8 ± 0.2 | 26.4 ± 0.1 | 216.0 | 216.0 |

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
| `handLensModify` | `-` | 2.8 ± 0.0 | — | 24.0 | — |
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
| `Get_3` | `-` | 16.2 ± 0.0 | 8.5 ± 0.6 | 0.0 | 0.0 |
| `Get_6` | `-` | 28.5 ± 0.1 | 24.8 ± 0.3 | 0.0 | 0.0 |
| `Get_orderId` | `-` | 0.9 ± 0.0 | 0.5 ± 0.0 | 0.0 | 0.0 |

## IsoBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 3.5 ± 0.0 | 3.6 ± 0.0 | 32.0 | 32.0 |
| `ReverseGet` | `-` | 3.1 ± 0.1 | 3.1 ± 0.0 | 32.0 | 32.0 |

## JsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `cModifyId` | `size=512` | 405,966.0 ± 4,763.1 | — | 1,066,767.5 | — |
| `cModifyId` | `size=64` | 50,984.9 ± 419.7 | — | 136,293.2 | — |
| `cModifyId` | `size=8` | 8,482.0 ± 66.9 | — | 20,712.1 | — |
| `cReadId` | `size=512` | 207,147.2 ± 2,600.4 | — | 797,930.9 | — |
| `cReadId` | `size=64` | 26,907.6 ± 1,026.2 | — | 101,295.0 | — |
| `cReadId` | `size=8` | 4,118.3 ± 161.9 | — | 15,568.0 | — |
| `cReadStreet` | `size=512` | 205,738.7 ± 2,827.3 | — | 797,930.5 | — |
| `cReadStreet` | `size=64` | 25,563.9 ± 284.3 | — | 101,293.5 | — |
| `cReadStreet` | `size=8` | 4,149.1 ± 65.2 | — | 15,568.0 | — |
| `cReplaceId` | `size=512` | 407,333.0 ± 5,789.5 | — | 1,066,694.9 | — |
| `cReplaceId` | `size=64` | 51,245.9 ± 445.9 | — | 136,245.3 | — |
| `cReplaceId` | `size=8` | 8,738.3 ± 49.2 | — | 20,664.1 | — |
| `cSumPrices` | `size=512` | 346,765.7 ± 6,676.3 | — | 1,240,742.7 | — |
| `cSumPrices` | `size=64` | 42,036.1 ± 321.6 | — | 157,043.0 | — |
| `cSumPrices` | `size=8` | 6,242.2 ± 88.1 | — | 22,720.1 | — |
| `jMiss` | `size=512` | 182.5 ± 3.2 | — | 0.1 | — |
| `jMiss` | `size=64` | 183.4 ± 3.0 | — | 0.0 | — |
| `jMiss` | `size=8` | 177.6 ± 4.0 | — | 0.0 | — |
| `jModifyId` | `size=512` | 3,925.8 ± 34.2 | — | 41,937.1 | — |
| `jModifyId` | `size=64` | 485.0 ± 5.5 | — | 5,328.0 | — |
| `jModifyId` | `size=8` | 113.4 ± 0.5 | — | 992.0 | — |
| `jReadId` | `size=512` | 34.4 ± 0.4 | — | 48.0 | — |
| `jReadId` | `size=64` | 34.1 ± 0.8 | — | 48.0 | — |
| `jReadId` | `size=8` | 34.6 ± 0.8 | — | 48.0 | — |
| `jReadStreet` | `size=512` | 194.1 ± 3.7 | — | 136.1 | — |
| `jReadStreet` | `size=64` | 193.5 ± 1.7 | — | 128.0 | — |
| `jReadStreet` | `size=8` | 198.9 ± 6.5 | — | 128.0 | — |
| `jReplaceId` | `size=512` | 3,842.0 ± 15.1 | — | 41,897.1 | — |
| `jReplaceId` | `size=64` | 475.4 ± 12.9 | — | 5,296.0 | — |
| `jReplaceId` | `size=8` | 109.8 ± 1.6 | — | 944.0 | — |
| `jSumPrices` | `size=512` | 85,230.2 ± 2,786.8 | — | 63,662.6 | — |
| `jSumPrices` | `size=64` | 11,232.2 ± 198.6 | — | 8,120.3 | — |
| `jSumPrices` | `size=8` | 1,456.0 ± 6.3 | — | 1,176.0 | — |

## KyoDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `EnvFocus` | `-` | 87.6 ± 3.2 | — | 320.0 | — |
| `MapDrillModify` | `-` | 38.8 ± 0.1 | — | 216.0 | — |
| `MapGet` | `-` | 2.4 ± 0.0 | — | 0.0 | — |
| `VarUpdateFocus` | `-` | 76.0 ± 15.1 | — | 216.0 | — |
| `handEnvUse` | `-` | 85.0 ± 0.2 | — | 312.0 | — |
| `handMapDrillModify` | `-` | 34.0 ± 0.5 | — | 216.0 | — |
| `handMapGet` | `-` | 1.9 ± 0.0 | — | 0.0 | — |
| `handVarUpdate` | `-` | 62.9 ± 2.4 | — | 200.0 | — |

## LensBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Get` | `-` | 1.0 ± 0.0 | 1.2 ± 0.0 | 0.0 | 0.0 |
| `Modify` | `-` | 4.1 ± 0.0 | 4.7 ± 0.0 | 40.0 | 40.0 |
| `ModifyDeep` | `-` | 32.9 ± 0.0 | 29.7 ± 0.1 | 152.0 | 176.0 |
| `Replace` | `-` | 3.4 ± 0.0 | 3.4 ± 0.0 | 40.0 | 40.0 |

## MultiFocusBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Fold_powerEach` | `size=1024` | 14,330.9 ± 545.3 | — | 43,035.7 | — |
| `Fold_powerEach` | `size=256` | 3,187.0 ± 64.0 | — | 9,240.2 | — |
| `Fold_powerEach` | `size=32` | 376.2 ± 5.7 | — | 920.0 | — |
| `Fold_powerEach` | `size=4` | 85.0 ± 0.7 | — | 328.0 | — |
| `Modify_multiFocus` | `size=1024` | 50,461.9 ± 2,229.6 | — | 331,119.6 | — |
| `Modify_multiFocus` | `size=256` | 11,261.9 ± 105.7 | — | 76,112.8 | — |
| `Modify_multiFocus` | `size=32` | 1,377.8 ± 27.6 | — | 8,600.0 | — |
| `Modify_multiFocus` | `size=4` | 223.1 ± 10.1 | — | 1,320.0 | — |
| `Modify_powerEach` | `size=1024` | 34,849.7 ± 533.5 | — | 115,177.0 | — |
| `Modify_powerEach` | `size=256` | 8,763.9 ± 808.5 | — | 26,080.6 | — |
| `Modify_powerEach` | `size=32` | 1,006.6 ± 9.6 | — | 3,152.0 | — |
| `Modify_powerEach` | `size=4` | 195.4 ± 3.4 | — | 800.0 | — |
| `naive_listMap` | `size=1024` | 8,359.2 ± 48.5 | — | 65,578.2 | — |
| `naive_listMap` | `size=256` | 2,079.7 ± 35.5 | — | 16,424.1 | — |
| `naive_listMap` | `size=32` | 250.4 ± 0.8 | — | 2,088.0 | — |
| `naive_listMap` | `size=4` | 32.9 ± 0.2 | — | 296.0 | — |
| `naive_sumQty` | `size=1024` | 4,133.4 ± 91.4 | — | 16,129.1 | — |
| `naive_sumQty` | `size=256` | 723.2 ± 35.7 | — | 3,840.0 | — |
| `naive_sumQty` | `size=32` | 58.3 ± 0.3 | — | 256.0 | — |
| `naive_sumQty` | `size=4` | 8.3 ± 0.4 | — | 0.0 | — |

## MultiFocusCollectBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `CollectList_listSum` | `-` | 32.7 ± 0.6 | — | 56.0 | — |
| `CollectMap_constSum` | `-` | 2.0 ± 0.0 | — | 0.0 | — |
| `CollectMap_zipMean` | `-` | 122.5 ± 0.6 | — | 880.0 | — |
| `Modify_multiFocusTuple3` | `-` | 8.7 ± 0.1 | — | 96.0 | — |
| `Modify_multiFocusTuple6` | `-` | 30.6 ± 0.1 | — | 248.0 | — |
| `naive_constSum` | `-` | 1.8 ± 0.0 | — | 16.0 | — |
| `naive_listSum` | `-` | 29.6 ± 0.1 | — | 56.0 | — |
| `naive_tuple3Rewrite` | `-` | 7.6 ± 0.1 | — | 96.0 | — |
| `naive_tuple6Rewrite` | `-` | 14.2 ± 0.1 | — | 184.0 | — |
| `naive_zipMeanBroadcast` | `-` | 156.6 ± 0.3 | — | 1,176.0 | — |

## OpticBuildBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `build` | `-` | 44.8 ± 0.4 | — | 184.0 | — |
| `buildAndUse` | `-` | 1,150.5 ± 34.8 | — | 2,888.0 | — |
| `reuseUse` | `-` | 1,064.0 ± 15.8 | — | 2,720.0 | — |

## OptionalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 20.1 ± 0.1 | 21.6 ± 0.2 | 88.0 | 112.0 |
| `Modify_0_empty` | `-` | 1.0 ± 0.0 | 1.4 ± 0.0 | 0.0 | 0.0 |
| `Modify_3` | `-` | 45.1 ± 0.1 | 71.2 ± 0.2 | 136.0 | 304.0 |
| `Modify_6` | `-` | 110.7 ± 1.7 | 120.4 ± 1.6 | 184.0 | 496.0 |
| `Modify_loyalty` | `-` | 15.0 ± 0.2 | 16.5 ± 0.3 | 88.0 | 112.0 |
| `Modify_loyalty_empty` | `-` | 1.2 ± 0.0 | 1.6 ± 0.0 | 0.0 | 0.0 |
| `Replace_0` | `-` | 4.5 ± 0.0 | 3.7 ± 0.0 | 40.0 | 40.0 |
| `Replace_loyalty` | `-` | 7.7 ± 0.1 | 7.2 ± 0.1 | 88.0 | 88.0 |

## OrderAvroBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyNames` | `size=512` | 33,623.2 ± 732.4 | — | 97,423.9 | — |
| `ModifyNames` | `size=64` | 3,979.0 ± 37.6 | — | 12,616.1 | — |
| `ModifyNames` | `size=8` | 545.7 ± 11.6 | — | 1,992.0 | — |
| `ModifyStreet` | `size=512` | 114.0 ± 5.1 | — | 328.0 | — |
| `ModifyStreet` | `size=64` | 110.8 ± 0.7 | — | 328.0 | — |
| `ModifyStreet` | `size=8` | 111.1 ± 0.9 | — | 328.0 | — |
| `ReadStreet` | `size=512` | 35.4 ± 0.7 | — | 88.0 | — |
| `ReadStreet` | `size=64` | 35.0 ± 0.2 | — | 88.0 | — |
| `ReadStreet` | `size=8` | 35.2 ± 0.2 | — | 88.0 | — |
| `monocleModifyNames` | `size=512` | 91,735.3 ± 641.8 | — | 382,775.2 | — |
| `monocleModifyNames` | `size=64` | 9,057.7 ± 94.1 | — | 39,848.3 | — |
| `monocleModifyNames` | `size=8` | 1,397.8 ± 10.0 | — | 5,400.0 | — |
| `monocleModifyStreet` | `size=512` | 46,063.3 ± 529.4 | — | 169,079.8 | — |
| `monocleModifyStreet` | `size=64` | 6,009.2 ± 39.0 | — | 20,896.2 | — |
| `monocleModifyStreet` | `size=8` | 917.0 ± 18.2 | — | 2,992.0 | — |
| `monocleReadStreet` | `size=512` | 26,735.9 ± 2,744.0 | — | 69,784.0 | — |
| `monocleReadStreet` | `size=64` | 3,225.9 ± 10.7 | — | 8,848.1 | — |
| `monocleReadStreet` | `size=8` | 475.1 ± 2.9 | — | 1,208.0 | — |
| `naiveModifyNames` | `size=512` | 60,472.3 ± 1,208.4 | — | 226,296.6 | — |
| `naiveModifyNames` | `size=64` | 7,659.7 ± 102.4 | — | 27,928.3 | — |
| `naiveModifyNames` | `size=8` | 1,103.1 ± 21.2 | — | 3,752.0 | — |
| `naiveModifyStreet` | `size=512` | 46,001.2 ± 276.3 | — | 169,056.8 | — |
| `naiveModifyStreet` | `size=64` | 6,004.5 ± 18.9 | — | 20,872.2 | — |
| `naiveModifyStreet` | `size=8` | 936.3 ± 11.4 | — | 2,968.0 | — |
| `naiveReadStreet` | `size=512` | 25,175.4 ± 415.2 | — | 69,783.3 | — |
| `naiveReadStreet` | `size=64` | 3,223.2 ± 21.3 | — | 8,848.1 | — |
| `naiveReadStreet` | `size=8` | 474.4 ± 3.5 | — | 1,208.0 | — |

## OrderCirceBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Names` | `size=512` | 252,132.2 ± 7,862.2 | — | 626,354.1 | — |
| `Names` | `size=64` | 30,613.8 ± 740.9 | — | 79,299.2 | — |
| `Names` | `size=8` | 4,307.6 ± 102.8 | — | 10,944.1 | — |
| `NamesIor` | `size=512` | 268,561.7 ± 4,270.1 | — | 691,767.2 | — |
| `NamesIor` | `size=64` | 33,341.6 ± 414.5 | — | 86,979.5 | — |
| `NamesIor` | `size=8` | 4,502.9 ± 63.7 | — | 11,488.1 | — |
| `Street` | `size=512` | 1,077.5 ± 24.9 | — | 2,720.9 | — |
| `Street` | `size=64` | 1,083.4 ± 25.8 | — | 2,720.1 | — |
| `Street` | `size=8` | 1,078.8 ± 28.7 | — | 2,720.0 | — |
| `StreetIor` | `size=512` | 1,060.2 ± 13.7 | — | 2,736.8 | — |
| `StreetIor` | `size=64` | 1,056.6 ± 12.2 | — | 2,736.1 | — |
| `StreetIor` | `size=8` | 1,127.0 ± 15.5 | — | 2,736.0 | — |
| `directNames` | `size=512` | 244,762.7 ± 2,170.1 | — | 605,788.2 | — |
| `directNames` | `size=64` | 29,903.5 ± 428.3 | — | 77,706.9 | — |
| `directNames` | `size=8` | 4,128.5 ± 61.1 | — | 10,688.1 | — |
| `directStreet` | `size=512` | 1,043.4 ± 17.2 | — | 2,744.8 | — |
| `directStreet` | `size=64` | 1,045.1 ± 22.2 | — | 2,728.1 | — |
| `directStreet` | `size=8` | 1,071.9 ± 20.1 | — | 2,736.0 | — |
| `hcursorNames` | `size=512` | 246,572.0 ± 3,012.7 | — | 605,789.6 | — |
| `hcursorNames` | `size=64` | 30,239.9 ± 757.5 | — | 77,792.3 | — |
| `hcursorNames` | `size=8` | 4,236.4 ± 76.4 | — | 10,696.1 | — |
| `hcursorStreet` | `size=512` | 1,128.1 ± 16.8 | — | 3,032.9 | — |
| `hcursorStreet` | `size=64` | 1,099.8 ± 9.9 | — | 3,032.1 | — |
| `hcursorStreet` | `size=8` | 1,141.9 ± 28.3 | — | 3,032.0 | — |
| `monocleNames` | `size=512` | 228,250.3 ± 4,270.1 | — | 1,121,761.3 | — |
| `monocleNames` | `size=64` | 23,951.4 ± 342.9 | — | 132,410.1 | — |
| `monocleNames` | `size=8` | 3,743.6 ± 76.2 | — | 19,472.1 | — |
| `monocleStreet` | `size=512` | 182,604.6 ± 5,567.4 | — | 908,032.0 | — |
| `monocleStreet` | `size=64` | 21,164.2 ± 578.6 | — | 113,455.2 | — |
| `monocleStreet` | `size=8` | 3,183.5 ± 29.2 | — | 17,058.7 | — |
| `naiveNames` | `size=512` | 193,826.6 ± 6,465.7 | — | 965,258.2 | — |
| `naiveNames` | `size=64` | 22,769.0 ± 320.4 | — | 120,834.4 | — |
| `naiveNames` | `size=8` | 3,381.1 ± 35.8 | — | 17,861.4 | — |
| `naiveStreet` | `size=512` | 182,689.6 ± 3,820.8 | — | 908,048.1 | — |
| `naiveStreet` | `size=64` | 21,260.5 ± 150.2 | — | 113,812.6 | — |
| `naiveStreet` | `size=8` | 3,185.8 ± 17.4 | — | 17,040.1 | — |

## OrderJsoniterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `ModifyStreet` | `size=512` | 4,073.2 ± 29.6 | — | 42,019.6 | — |
| `ModifyStreet` | `size=64` | 678.7 ± 11.2 | — | 5,432.1 | — |
| `ModifyStreet` | `size=8` | 300.7 ± 8.4 | — | 1,072.0 | — |
| `ReadStreet` | `size=512` | 195.6 ± 2.9 | — | 128.2 | — |
| `ReadStreet` | `size=64` | 195.3 ± 1.4 | — | 128.0 | — |
| `ReadStreet` | `size=8` | 193.5 ± 3.6 | — | 90.7 | — |
| `SumPrices` | `size=512` | 84,198.6 ± 1,424.5 | — | 63,715.5 | — |
| `SumPrices` | `size=64` | 10,822.5 ± 305.8 | — | 8,121.3 | — |
| `SumPrices` | `size=8` | 1,472.2 ± 16.6 | — | 1,176.0 | — |
| `monocleModifyStreet` | `size=512` | 158,513.7 ± 1,401.7 | — | 333,529.1 | — |
| `monocleModifyStreet` | `size=64` | 18,795.9 ± 110.4 | — | 30,081.9 | — |
| `monocleModifyStreet` | `size=8` | 3,179.4 ± 21.2 | — | 4,664.1 | — |
| `monocleReadStreet` | `size=512` | 89,185.5 ± 1,919.3 | — | 193,228.3 | — |
| `monocleReadStreet` | `size=64` | 11,396.0 ± 204.5 | — | 24,705.2 | — |
| `monocleReadStreet` | `size=8` | 1,807.9 ± 26.6 | — | 3,648.0 | — |
| `monocleSumPrices` | `size=512` | 419,529.2 ± 4,049.7 | — | 1,178,440.4 | — |
| `monocleSumPrices` | `size=64` | 15,575.2 ± 405.5 | — | 46,329.6 | — |
| `monocleSumPrices` | `size=8` | 2,438.1 ± 33.7 | — | 6,448.1 | — |
| `naiveModifyStreet` | `size=512` | 157,060.2 ± 764.3 | — | 333,563.2 | — |
| `naiveModifyStreet` | `size=64` | 19,119.8 ± 335.6 | — | 30,057.9 | — |
| `naiveModifyStreet` | `size=8` | 3,164.1 ± 21.0 | — | 4,640.1 | — |
| `naiveReadStreet` | `size=512` | 88,209.9 ± 664.8 | — | 193,227.5 | — |
| `naiveReadStreet` | `size=64` | 11,490.8 ± 91.6 | — | 24,705.2 | — |
| `naiveReadStreet` | `size=8` | 1,781.1 ± 19.1 | — | 3,648.0 | — |
| `naiveSumPrices` | `size=512` | 94,086.5 ± 1,842.2 | — | 230,120.5 | — |
| `naiveSumPrices` | `size=64` | 12,169.2 ± 81.6 | — | 29,337.2 | — |
| `naiveSumPrices` | `size=8` | 1,896.5 ± 36.7 | — | 4,248.0 | — |
| `nativeReadStreet` | `size=512` | 33,227.0 ± 438.6 | — | 451.6 | — |
| `nativeReadStreet` | `size=64` | 4,300.4 ± 113.2 | — | 424.5 | — |
| `nativeReadStreet` | `size=8` | 848.8 ± 51.9 | — | 413.4 | — |
| `nativeSumPrices` | `size=512` | 61,854.5 ± 158.1 | — | 86,270.6 | — |
| `nativeSumPrices` | `size=64` | 7,667.6 ± 39.8 | — | 10,920.8 | — |
| `nativeSumPrices` | `size=8` | 1,153.9 ± 14.9 | — | 1,512.0 | — |

## PlatedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `TransformDeep` | `n=4096` | 118,961.4 ± 841.5 | — | 624,382.6 | — |
| `TransformDeep` | `n=512` | 11,713.2 ± 64.0 | — | 57,361.2 | — |
| `TransformDeep` | `n=64` | 1,423.6 ± 4.3 | — | 7,184.0 | — |
| `TransformExpr` | `n=4096` | 128,909.8 ± 464.3 | 163,201.5 ± 731.0 | 655,357.8 | 753,734.8 |
| `TransformExpr` | `n=512` | 15,923.9 ± 57.0 | 15,215.1 ± 77.9 | 81,825.6 | 69,585.6 |
| `TransformExpr` | `n=64` | 1,997.9 ± 11.7 | 2,493.3 ± 14.1 | 10,144.0 | 11,728.0 |
| `UniverseDeep` | `n=4096` | 101,528.6 ± 6,372.0 | — | 786,577.9 | — |
| `UniverseDeep` | `n=512` | 15,210.7 ± 54.3 | — | 98,377.6 | — |
| `UniverseDeep` | `n=64` | 1,862.5 ± 4.4 | — | 12,360.0 | — |
| `UniverseExpr` | `n=4096` | 116,294.5 ± 28,440.5 | 2,850,154.9 ± 175,954.7 | 786,396.6 | 4,687,752.0 |
| `UniverseExpr` | `n=512` | 14,773.3 ± 74.3 | 202,138.9 ± 6,853.9 | 98,185.5 | 475,012.7 |
| `UniverseExpr` | `n=64` | 1,800.3 ± 5.8 | 17,961.3 ± 636.6 | 12,168.0 | 45,424.3 |
| `UniverseJson` | `n=4096` | 219,072.1 ± 1,496.9 | 3,165,379.8 ± 127,867.1 | 786,471.4 | 6,467,998.3 |
| `UniverseJson` | `n=512` | 24,697.4 ± 73.7 | 238,622.5 ± 4,742.1 | 98,186.5 | 699,920.4 |
| `UniverseJson` | `n=64` | 2,979.0 ± 6.5 | 23,085.1 ± 724.4 | 12,168.1 | 73,208.4 |
| `visitorTransformDeep` | `n=4096` | 42,376.9 ± 214.6 | — | 163,886.8 | — |
| `visitorTransformDeep` | `n=512` | 4,899.4 ± 22.2 | — | 20,496.5 | — |
| `visitorTransformDeep` | `n=64` | 432.7 ± 9.7 | — | 2,576.0 | — |
| `visitorTransformExpr` | `n=4096` | 64,367.4 ± 368.5 | — | 360,470.9 | — |
| `visitorTransformExpr` | `n=512` | 7,587.9 ± 111.0 | — | 45,032.8 | — |
| `visitorTransformExpr` | `n=64` | 988.3 ± 8.9 | — | 5,608.0 | — |
| `visitorUniverseDeep` | `n=4096` | 54,367.3 ± 484.4 | — | 196,703.6 | — |
| `visitorUniverseDeep` | `n=512` | 6,484.9 ± 53.8 | — | 24,632.7 | — |
| `visitorUniverseDeep` | `n=64` | 747.4 ± 2.1 | — | 3,128.0 | — |
| `visitorUniverseExpr` | `n=4096` | 52,945.1 ± 303.6 | — | 196,654.5 | — |
| `visitorUniverseExpr` | `n=512` | 6,521.2 ± 13.7 | — | 24,584.7 | — |
| `visitorUniverseExpr` | `n=64` | 800.8 ± 9.7 | — | 3,080.0 | — |
| `visitorUniverseJson` | `n=4096` | 103,026.3 ± 1,410.5 | — | 196,707.0 | — |
| `visitorUniverseJson` | `n=512` | 9,225.0 ± 33.7 | — | 24,600.9 | — |
| `visitorUniverseJson` | `n=64` | 1,345.6 ± 3.7 | — | 3,096.0 | — |

## PowerSeriesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_powerEach` | `size=1024` | 12,168.9 ± 47.2 | — | 41,413.8 | — |
| `Modify_powerEach` | `size=16` | 256.5 ± 0.9 | — | 1,088.0 | — |
| `Modify_powerEach` | `size=256` | 2,984.9 ± 51.4 | — | 10,688.5 | — |
| `Modify_powerEach` | `size=4` | 123.0 ± 0.4 | — | 608.0 | — |
| `Modify_powerEach` | `size=4096` | 51,891.8 ± 2,141.1 | — | 164,372.5 | — |
| `Modify_powerEach` | `size=64` | 787.8 ± 3.5 | — | 3,008.0 | — |
| `monocle_powerEach` | `size=1024` | 60,820.5 ± 649.1 | — | 279,433.4 | — |
| `monocle_powerEach` | `size=16` | 604.6 ± 2.1 | — | 3,720.0 | — |
| `monocle_powerEach` | `size=256` | 21,967.3 ± 447.8 | — | 107,339.4 | — |
| `monocle_powerEach` | `size=4` | 208.7 ± 0.7 | — | 1,176.0 | — |
| `monocle_powerEach` | `size=4096` | 180,527.2 ± 2,840.2 | — | 967,846.0 | — |
| `monocle_powerEach` | `size=64` | 2,165.2 ± 28.0 | — | 14,504.1 | — |
| `naive_powerEach` | `size=1024` | 7,001.3 ± 839.4 | — | 28,731.2 | — |
| `naive_powerEach` | `size=16` | 101.2 ± 0.3 | — | 504.0 | — |
| `naive_powerEach` | `size=256` | 1,623.1 ± 9.8 | — | 7,224.3 | — |
| `naive_powerEach` | `size=4` | 25.2 ± 0.4 | — | 168.0 | — |
| `naive_powerEach` | `size=4096` | 26,928.5 ± 80.7 | — | 114,787.9 | — |
| `naive_powerEach` | `size=64` | 396.3 ± 0.5 | — | 1,848.0 | — |

## PowerSeriesNestedBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_nested` | `size=1024` | 70,461.3 ± 5,148.6 | — | 210,496.3 | — |
| `Modify_nested` | `size=16` | 1,458.9 ± 11.7 | — | 4,768.1 | — |
| `Modify_nested` | `size=256` | 17,691.0 ± 175.1 | — | 53,675.0 | — |
| `Modify_nested` | `size=4` | 702.3 ± 8.7 | — | 2,264.0 | — |
| `Modify_nested` | `size=64` | 4,583.4 ± 12.0 | — | 14,616.5 | — |
| `monocle_nested` | `size=1024` | 248,988.1 ± 3,701.7 | — | 1,118,853.3 | — |
| `monocle_nested` | `size=16` | 2,670.6 ± 22.1 | — | 15,722.8 | — |
| `monocle_nested` | `size=256` | 93,248.1 ± 1,199.4 | — | 430,209.0 | — |
| `monocle_nested` | `size=4` | 1,227.9 ± 32.3 | — | 5,568.0 | — |
| `monocle_nested` | `size=64` | 8,997.6 ± 61.1 | — | 58,833.8 | — |
| `naive_nested` | `size=1024` | 21,717.8 ± 250.4 | — | 115,071.5 | — |
| `naive_nested` | `size=16` | 365.0 ± 2.8 | — | 2,136.0 | — |
| `naive_nested` | `size=256` | 5,244.2 ± 34.8 | — | 29,019.3 | — |
| `naive_nested` | `size=4` | 126.2 ± 2.4 | — | 792.0 | — |
| `naive_nested` | `size=64` | 1,321.7 ± 8.1 | — | 7,512.2 | — |

## PowerSeriesPrismBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_sparse` | `size=128` | 1,395.7 ± 107.0 | — | 4,816.1 | — |
| `Modify_sparse` | `size=2048` | 23,478.8 ± 731.2 | — | 104,683.8 | — |
| `Modify_sparse` | `size=32` | 347.8 ± 2.6 | — | 1,360.0 | — |
| `Modify_sparse` | `size=512` | 5,682.8 ± 80.6 | — | 24,785.6 | — |
| `Modify_sparse` | `size=8` | 119.0 ± 0.5 | — | 496.0 | — |
| `monocle_sparse` | `size=128` | 3,868.3 ± 16.7 | — | 27,808.2 | — |
| `monocle_sparse` | `size=2048` | 106,899.8 ± 1,672.8 | — | 523,146.7 | — |
| `monocle_sparse` | `size=32` | 1,050.2 ± 8.0 | — | 7,040.0 | — |
| `monocle_sparse` | `size=512` | 33,507.8 ± 316.4 | — | 166,686.0 | — |
| `monocle_sparse` | `size=8` | 348.3 ± 7.0 | — | 1,952.0 | — |
| `naive_sparse` | `size=128` | 287.4 ± 1.8 | — | 1,568.0 | — |
| `naive_sparse` | `size=2048` | 4,951.6 ± 21.3 | — | 24,612.1 | — |
| `naive_sparse` | `size=32` | 74.2 ± 0.5 | — | 416.0 | — |
| `naive_sparse` | `size=512` | 1,162.9 ± 6.5 | — | 6,176.3 | — |
| `naive_sparse` | `size=8` | 25.0 ± 0.3 | — | 128.0 | — |

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
| `ReverseGet_6` | `-` | 34.2 ± 0.3 | — | 120.0 | — |
| `naiveBuild_0` | `-` | 2.3 ± 0.0 | — | 24.0 | — |
| `naiveBuild_3` | `-` | 6.7 ± 0.1 | — | 72.0 | — |
| `naiveBuild_6` | `-` | 11.1 ± 0.0 | — | 120.0 | — |

## SchemesBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Ana` | `-` | 96,481.2 ± 434.9 | — | 589,712.7 | — |
| `Cata` | `-` | 68,500.4 ± 637.3 | — | 197,568.5 | — |
| `Hylo` | `-` | 90,046.2 ± 688.4 | — | 295,848.6 | — |
| `drosteAna` | `-` | 46,362.9 ± 341.2 | — | 327,632.3 | — |
| `drosteCata` | `-` | 38,267.7 ± 458.3 | — | 164,824.3 | — |
| `drosteHylo` | `-` | 64,397.9 ± 880.4 | — | 328,640.5 | — |
| `handAna` | `-` | 20,847.4 ± 518.0 | — | 163,816.1 | — |
| `handCata` | `-` | 12,553.0 ± 29.6 | — | 0.1 | — |
| `handHylo` | `-` | 9,529.6 ± 45.1 | — | 0.1 | — |

## SetterBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `Modify_0` | `-` | 2.4 ± 0.0 | 2.4 ± 0.0 | 24.0 | 24.0 |
| `Modify_3` | `-` | 11.3 ± 0.1 | 28.1 ± 0.4 | 72.0 | 168.0 |
| `Modify_6` | `-` | 23.8 ± 0.3 | 65.6 ± 2.0 | 120.0 | 288.0 |
| `Modify_orderId` | `-` | 3.3 ± 0.0 | 3.3 ± 0.0 | 40.0 | 40.0 |

## TraversalBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `FoldNested` | `size=512` | 4,993.0 ± 153.0 | — | 20,200.8 | — |
| `FoldNested` | `size=64` | 533.0 ± 1.9 | — | 2,680.0 | — |
| `FoldNested` | `size=8` | 49.0 ± 0.4 | — | 328.0 | — |
| `FoldPrices` | `size=512` | 1,910.6 ± 25.1 | 31,759.3 ± 419.1 | 12,312.3 | 162,565.0 |
| `FoldPrices` | `size=64` | 156.2 ± 0.6 | 2,074.6 ± 14.1 | 1,560.0 | 15,408.1 |
| `FoldPrices` | `size=8` | 25.7 ± 0.1 | 324.8 ± 4.7 | 216.0 | 2,000.0 |
| `Modify` | `size=512` | 7,287.8 ± 193.0 | 33,685.0 ± 303.5 | 36,897.2 | 176,891.2 |
| `Modify` | `size=64` | 845.4 ± 2.2 | 1,802.9 ± 10.5 | 4,640.0 | 14,408.0 |
| `Modify` | `size=8` | 95.7 ± 1.3 | 286.3 ± 3.3 | 608.0 | 1,896.0 |

## ZioDiBench

| Benchmark | params | eo ns/op | monocle ns/op | eo B/op | monocle B/op |
|---|---|---:|---:|---:|---:|
| `DrillGet` | `-` | 10.3 ± 0.0 | — | 0.0 | — |
| `DrillModify` | `-` | 94.9 ± 0.8 | — | 472.0 | — |
| `ServiceGet` | `-` | 5.3 ± 0.0 | — | 0.0 | — |
| `ServiceReplace` | `-` | 66.2 ± 0.7 | — | 400.0 | — |
| `handDrillGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handDrillModify` | `-` | 92.0 ± 1.2 | — | 592.0 | — |
| `handServiceGet` | `-` | 14.0 ± 0.1 | — | 120.0 | — |
| `handServiceReplace` | `-` | 75.0 ± 1.0 | — | 520.0 | — |


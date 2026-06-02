# vskedhuman
## Describe the human body using the Java programming language from the perspective of the five fundamental anatomical regions.

human emulator with java  

Physiological systems (e.g., cardiovascular, respiratory)  
Neural activity (e.g., simplified neural networks)  
Pharmacokinetics (the distribution of drugs in the body)  
Human movement and biomechanics  
Virtual patients (for medical education or simulation)  


## 20260602
add dna base class A, C, G, T  
A & T has 2 hydrogen bonds
C & G has 3 hydrogen bonds more tight than 2 hydrogen bonds

这四个字母可以认为是四个汉字笔画。如横竖撇捺

every 3 letters construct a word called Codon.

| DNA 上的 3 个字母 (源代码/密码子) | 细胞去仓库拿出的氨基酸 (实例化对象) | 该氨基酸的英文缩写 (方便人类记录) |
| :--- | :--- | :--- |
| **ATG** | 甲硫氨酸 | **Met** (Methionine) ⭐起始信号 |
| **TTT, TTC** | 苯丙氨酸 | **Phe** (Phenylalanine) |
| **TTA, TTG, CTT, CTC, CTA, CTG** | 亮氨酸 | **Leu** (Leucine) |
| **ATT, ATC, ATA** | 异亮氨酸 | **Ile** (Isoleucine) |
| **GTT, GTC, GTA, GTG** | 缬氨酸 | **Val** (Valine) |
| **TCT, TCC, TCA, TCG, AGT, AGC** | 丝氨酸 | **Ser** (Serine) |
| **CCT, CCC, CCA, CCG** | 脯氨酸 | **Pro** (Proline) |
| **ACT, ACC, ACA, ACG** | 苏氨酸 | **Thr** (Threonine) |
| **GCT, GCC, GCA, GCG** | 丙氨酸 | **Ala** (Alanine) |
| **TAT, TAC** | 酪氨酸 | **Tyr** (Tyrosine) |
| **CAT, CAC** | 组氨酸 | **His** (Histidine) |
| **CAA, CAG** | 谷氨酰胺 | **Gln** (Glutamine) |
| **AAT, AAC** | 天冬酰胺 | **Asn** (Asparagine) |
| **AAA, AAG** | 赖氨酸 | **Lys** (Lysine) |
| **GAT, GAC** | 天冬氨酸 | **Asp** (Aspartic acid) |
| **GAA, GAG** | 谷氨酸 | **Glu** (Glutamic acid) |
| **TGT, TGC** | 半胱氨酸 | **Cys** (Cysteine) |
| **TGG** | 色氨酸 | **Trp** (Tryptophan) |
| **CGT, CGC, CGA, CGG, AGA, AGG** | 精氨酸 | **Arg** (Arginine) |
| **GGT, GGC, GGA, GGG** | 甘氨酸 | **Gly** (Glycine) |
| **TAA, TAG, TGA** | ⛔ **终止信号** (不拿氨基酸，停止合成) | **Stop** |

## 20251104
add human id and name
init project use java language

### TODO list
Human body → Organ systems → Organs → Tissues → Cells  


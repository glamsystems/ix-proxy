# ixProxy

Facilitates the re-mapping of instructions from one program to another proxy program. The primary use case to add
additional safety checks in the proxy program before forwarding the request to the original program.

## Configuration

```json
{
  "program_id": "dRiftyHA39MWEi3m9aunc5MzRF1JYuBsbn6VPcn33UH",
  "instructions": [
    {
      "src_ix_name": "cancel_orders",
      "src_discriminator": [
        238,
        225,
        95,
        158,
        227,
        103,
        8,
        194
      ],
      "dst_ix_name": "drift_cancel_orders",
      "dst_discriminator": [
        98,
        107,
        48,
        79,
        97,
        60,
        99,
        58
      ],
      "dynamic_accounts": [
        {
          "name": "glam_state",
          "index": 0,
          "writable": false,
          "signer": false
        },
        {
          "name": "glam_vault",
          "index": 1,
          "writable": false,
          "signer": false
        },
        {
          "name": "glam_signer",
          "index": 2,
          "writable": true,
          "signer": true
        },
        {
          "name": "cpi_program",
          "index": 3,
          "writable": false,
          "signer": false
        }
      ],
      "static_accounts": [],
      "index_map": [
        4,
        5,
        -1
      ]
    }
  ]
}
```

### **src_discriminator**

The instruction discriminator of the original instruction.

### **dst_discriminator**

The instruction discriminator of the proxy instruction.

### **dynamic_accounts**

Account Meta information for accounts which can differ at runtime.

### **static_accounts**

Account Meta information for accounts which are always the same given a Solana cluster such as mainnet.

### **index_map**

Defines the parameter index for the destination instruction. If the account has been removed or replaced use a negative
number.

## Usage

### Construct IxProxy Map

Parse a `ProgramMapConfig` and construct an IxProxy for each instruction. With the `ixProxyMap`, IxProxy's can be
retrieved given the source instruction discriminator and then used to translate the instruction to the proxy program.

```java
var mappingJson = "";
var ji = JsonIterator.parse(mappingJson);

var programMapConfig = ProgramMapConfig.parseConfig(ji);
var program = programMapConfig.program();
var programAccountMeta = AccountMeta.createRead(program);

var ixMapConfigs = programMapConfig.ixMapConfigs();

record GlamVaultAccounts(AccountMeta readGlamState,
                         AccountMeta writeGlamState,
                         AccountMeta readGlamVault,
                         AccountMeta writeGlamVault) {

}


var ixProxyMap = HashMap.<Discriminator, IxProxy<GlamVaultAccounts>>newHashMap(ixMapConfigs.size());

for (var ixMapConfig : ixMapConfigs) {
  Function<DynamicAccountConfig, DynamicAccount<GlamVaultAccounts>> dynamicAccountFactory = accountConfig -> {
    int index = accountConfig.index();
    boolean w = accountConfig.writable();
    return switch (accountConfig.name()) {
      case "glam_state" -> (mappedAccounts, _, vaultAccounts) -> mappedAccounts[index] = w
          ? vaultAccounts.writeGlamState() : vaultAccounts.readGlamState();
      case "glam_vault" -> (mappedAccounts, _, vaultAccounts) -> mappedAccounts[index] = w
          ? vaultAccounts.writeGlamVault() : vaultAccounts.readGlamVault();
      case "glam_signer" -> accountConfig.createFeePayerAccount();
      case "cpi_program" -> accountConfig.createDynamicAccount(programAccountMeta);
      default -> throw new IllegalStateException("Unknown dynamic account type: " + accountConfig.name());
    };
  };

  var srcDiscriminator = ixMapConfig.srcDiscriminator();
  var ixProxy = ixMapConfig.createProxy(dynamicAccountFactory);
  ixProxyMap.put(srcDiscriminator, ixProxy);
}
```

### Translate Source Program Instruction

```java
AccountMeta feePayer = AccountMeta.createFeePayer(PublicKey.fromBase58Encoded(""));

PublicKey glamStateAccount = PublicKey.fromBase58Encoded("");
GlamVaultAccounts vaultAccounts = GlamVaultAccounts.create(glamStateAccount);

Instruction sourceIx = null;
IxProxyRecord<GlamVaultAccounts> ixProxy = ixProxyMap.g;
Instruction mappedIx = ixProxy.mapInstruction(feePayer, vaultAccounts, sourceIx);
```

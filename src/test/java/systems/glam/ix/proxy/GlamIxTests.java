package systems.glam.ix.proxy;

import org.junit.jupiter.api.Test;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.JsonIterator;

import java.util.HashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

final class GlamIxTests {

  @Test
  void testGlamIx() {
    final var mappingJson = """
        {
          "program_id": "dRiftyHA39MWEi3m9aunc5MzRF1JYuBsbn6VPcn33UH",
          "instructions": [
            {
              "src_ix_name": "initialize_user",
              "src_discriminator": [
                111,
                17,
                185,
                250,
                60,
                122,
                38,
                254
              ],
              "dst_ix_name": "drift_initialize_user",
              "dst_discriminator": [
                107,
                244,
                158,
                15,
                225,
                239,
                98,
                245
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
                6,
                -1,
                7,
                8,
                9
              ]
            },
            {
              "src_ix_name": "initialize_user_stats",
              "src_discriminator": [
                254,
                243,
                72,
                98,
                251,
                130,
                168,
                213
              ],
              "dst_ix_name": "drift_initialize_user_stats",
              "dst_discriminator": [
                133,
                185,
                103,
                162,
                90,
                161,
                78,
                143
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
                -1,
                6,
                7,
                8
              ]
            },
            {
              "src_ix_name": "deposit",
              "src_discriminator": [
                242,
                35,
                198,
                137,
                82,
                225,
                242,
                182
              ],
              "dst_ix_name": "drift_deposit",
              "dst_discriminator": [
                252,
                63,
                250,
                201,
                98,
                55,
                130,
                12
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
                  "writable": true,
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
                6,
                -1,
                7,
                8,
                9
              ]
            },
            {
              "src_ix_name": "withdraw",
              "src_discriminator": [
                183,
                18,
                70,
                156,
                148,
                109,
                161,
                34
              ],
              "dst_ix_name": "drift_withdraw",
              "dst_discriminator": [
                86,
                59,
                186,
                123,
                183,
                181,
                234,
                137
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
                  "writable": true,
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
                6,
                -1,
                7,
                8,
                9,
                10
              ]
            },
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
            },
            {
              "src_ix_name": "modify_order",
              "src_discriminator": [
                47,
                124,
                117,
                255,
                201,
                197,
                130,
                94
              ],
              "dst_ix_name": "drift_modify_order",
              "dst_discriminator": [
                235,
                245,
                222,
                58,
                245,
                128,
                19,
                202
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
            },
            {
              "src_ix_name": "update_user_custom_margin_ratio",
              "src_discriminator": [
                21,
                221,
                140,
                187,
                32,
                129,
                11,
                123
              ],
              "dst_ix_name": "drift_update_user_custom_margin_ratio",
              "dst_discriminator": [
                4,
                47,
                193,
                177,
                128,
                62,
                228,
                14
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
                -1
              ]
            },
            {
              "src_ix_name": "delete_user",
              "src_discriminator": [
                186,
                85,
                17,
                249,
                219,
                231,
                98,
                251
              ],
              "dst_ix_name": "drift_delete_user",
              "dst_discriminator": [
                179,
                118,
                20,
                212,
                145,
                146,
                49,
                130
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
                  "writable": true,
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
                6,
                -1
              ]
            }
          ]
        }""";

    final var invokedProxyProgram = AccountMeta.createInvoked(PublicKey.NONE);
    final var ji = JsonIterator.parse(mappingJson);

    final var accountMetaCache = new HashMap<AccountMeta, AccountMeta>();
    final var indexedAccountMetaCache = new HashMap<IndexedAccountMeta, IndexedAccountMeta>();

    final var programMapConfig = ProgramMapConfig.parseConfig(accountMetaCache, indexedAccountMetaCache, ji);
    final var readCpiProgram = programMapConfig.readCpiProgram();

    final var ixMapConfigs = programMapConfig.ixMapConfigs();

    record GlamVaultAccounts(AccountMeta readGlamState,
                             AccountMeta writeGlamState,
                             AccountMeta readGlamVault,
                             AccountMeta writeGlamVault) {

    }

    final var ixProxies = HashMap.<Discriminator, IxProxy<GlamVaultAccounts>>newHashMap(ixMapConfigs.size());

    for (final var ixMapConfig : ixMapConfigs) {
      final Function<DynamicAccountConfig, DynamicAccount<GlamVaultAccounts>> dynamicAccountFactory = accountConfig -> {
        final int index = accountConfig.index();
        final boolean w = accountConfig.writable();
        return switch (accountConfig.name()) {
          case "glam_state" -> (mappedAccounts, _, _, vaultAccounts) -> mappedAccounts[index] = w
              ? vaultAccounts.writeGlamState() : vaultAccounts.readGlamState();
          case "glam_vault" -> (mappedAccounts, _, _, vaultAccounts) -> mappedAccounts[index] = w
              ? vaultAccounts.writeGlamVault() : vaultAccounts.readGlamVault();
          case "glam_signer" -> accountConfig.createFeePayerAccount();
          case "cpi_program" -> accountConfig.createReadCpiProgram();
          default -> throw new IllegalStateException("Unknown dynamic account type: " + accountConfig.name());
        };
      };

      final var cpiDiscriminator = ixMapConfig.cpiDiscriminator();
      final var ixProxy = ixMapConfig.createProxy(readCpiProgram, invokedProxyProgram, dynamicAccountFactory);
      ixProxies.put(cpiDiscriminator, ixProxy);
    }

    assertEquals(8, ixMapConfigs.size());

    var ixProxy = ixProxies.get(Discriminator.toDiscriminator(186, 85, 17, 249, 219, 231, 98, 251));
    assertNotNull(ixProxy);

    assertEquals(
        Discriminator.toDiscriminator(179, 118, 20, 212, 145, 146, 49, 130),
        ixProxy.proxyDiscriminator()
    );

    assertInstanceOf(IxProxyRecord.class, ixProxy);
    final var ixProxyRecord = (IxProxyRecord<GlamVaultAccounts>) ixProxy;

    var dynamicAccounts = ixProxyRecord.dynamicAccounts();
    assertEquals(4, dynamicAccounts.size());

    var staticAccounts = ixProxyRecord.staticAccounts();
    assertEquals(0, staticAccounts.size());

    assertArrayEquals(
        new int[]{4, 5, 6, -1},
        ixProxyRecord.indexes()
    );
  }
}

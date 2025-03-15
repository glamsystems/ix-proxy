package systems.glam.ix.proxy;

import org.junit.jupiter.api.Test;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class GlamIxTests {

  record GlamVaultAccounts(AccountMeta readGlamState,
                           AccountMeta writeGlamState,
                           AccountMeta readGlamVault,
                           AccountMeta writeGlamVault) {

  }

  private static Collection<ProgramProxy<GlamVaultAccounts>> createProxies(final PublicKey invokedProxyProgram,
                                                                           final String mappingJson) {
    final var ji = JsonIterator.parse(mappingJson);

    final var accountMetaCache = new HashMap<AccountMeta, AccountMeta>();
    final var indexedAccountMetaCache = new HashMap<IndexedAccountMeta, IndexedAccountMeta>();

    final var programMapConfig = ProgramMapConfig.parseConfig(accountMetaCache, indexedAccountMetaCache, ji);

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
    return programMapConfig.createProgramProxies(AccountMeta.createInvoked(invokedProxyProgram), dynamicAccountFactory);
  }

  @Test
  void testPayerProxy() {
    final var mappingJson = """
        {
          "program_id": "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL",
          "instructions": [
            {
              "src_ix_name": "create",
              "src_discriminator": [
                0
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_signer",
                  "index": 0,
                  "writable": true,
                  "signer": true
                }
              ],
              "index_map": [
                -1,
                1,
                2,
                3,
                4,
                5
              ]
            },
            {
              "src_ix_name": "create_idempotent",
              "src_discriminator": [
                1
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_signer",
                  "index": 0,
                  "writable": true,
                  "signer": true
                }
              ],
              "index_map": [
                -1,
                1,
                2,
                3,
                4,
                5
              ]
            },
            {
              "src_ix_name": "recover_nested",
              "src_discriminator": [
                2
              ]
            }
          ]
        }""";

    final var programProxies = createProxies(PublicKey.NONE, mappingJson);
    assertEquals(1, programProxies.size());
    final var programProxy = programProxies.iterator().next();

    var ixProxy = programProxy.lookupProxy(Discriminator.toDiscriminator(0));
    assertNotNull(ixProxy);
    assertEquals(ixProxy.proxyDiscriminator(), ixProxy.cpiDiscriminator());

    assertInstanceOf(PayerIxProxy.class, ixProxy);
    var payerProxy = (PayerIxProxy<GlamVaultAccounts>) ixProxy;
    assertEquals(0, payerProxy.payerIndex);
  }

  @Test
  void testGlamDriftRemapping() {
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

    final var programProxies = createProxies(PublicKey.NONE, mappingJson);
    assertEquals(1, programProxies.size());
    final var programProxy = programProxies.iterator().next();

    var ixProxy = programProxy.lookupProxy(Discriminator.toDiscriminator(186, 85, 17, 249, 219, 231, 98, 251));
    assertNotNull(ixProxy);

    assertEquals(
        Discriminator.toDiscriminator(179, 118, 20, 212, 145, 146, 49, 130),
        ixProxy.proxyDiscriminator()
    );

    assertInstanceOf(IxProxyRecord.class, ixProxy);
    final var ixProxyRecord = (IxProxyRecord<GlamVaultAccounts>) ixProxy;

    var dynamicAccounts = ixProxyRecord.dynamicAccounts;
    assertEquals(4, dynamicAccounts.size());

    var staticAccounts = ixProxyRecord.staticAccounts;
    assertEquals(0, staticAccounts.size());

    assertArrayEquals(
        new int[]{4, 5, 6, -1},
        ixProxyRecord.indexes
    );
  }

  @Test
  void testGlamMarinadeUnstake() {
    final var systemProgramMappingJson = """
        {
          "program_id": "11111111111111111111111111111111",
          "instructions": [
            {
              "type": "payer",
              "src_ix_name": "create_account",
              "src_discriminator": [
                0,
                0,
                0,
                0
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_signer",
                  "index": 0,
                  "writable": true,
                  "signer": true
                }
              ],
              "index_map": [
                -1,
                1
              ]
            },
            {
              "src_ix_name": "assign",
              "src_discriminator": [
                1,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "transfer",
              "src_discriminator": [
                2,
                0,
                0,
                0
              ],
              "dst_ix_name": "system_transfer",
              "dst_discriminator": [
                167,
                164,
                195,
                155,
                219,
                152,
                191,
                230
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
              "index_map": [
                -1,
                4
              ]
            },
            {
              "type": "payer",
              "src_ix_name": "create_account_with_seed",
              "src_discriminator": [
                3,
                0,
                0,
                0
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_signer",
                  "index": 0,
                  "writable": true,
                  "signer": true
                }
              ],
              "index_map": [
                -1,
                1
              ]
            },
            {
              "src_ix_name": "advance_nonce_account",
              "src_discriminator": [
                4,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "withdraw_nonce_account",
              "src_discriminator": [
                5,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "initialize_nonce_account",
              "src_discriminator": [
                6,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "authorize_nonce_account",
              "src_discriminator": [
                7,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "allocate",
              "src_discriminator": [
                8,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "allocate_with_seed",
              "src_discriminator": [
                9,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "assign_with_seed",
              "src_discriminator": [
                10,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "transfer_with_seed",
              "src_discriminator": [
                11,
                0,
                0,
                0
              ]
            },
            {
              "src_ix_name": "upgrade_nonce_account",
              "src_discriminator": [
                12,
                0,
                0,
                0
              ]
            }
          ]
        }
        """;

    final var marinadeMappingJson = """
        {
          "program_id": "MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD",
          "instructions": [
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
              "dst_ix_name": "marinade_deposit",
              "dst_discriminator": [
                62,
                236,
                248,
                28,
                222,
                232,
                182,
                73
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
                7,
                8,
                9,
                -1,
                10,
                11,
                12,
                13
              ]
            },
            {
              "src_ix_name": "deposit_stake_account",
              "src_discriminator": [
                110,
                130,
                115,
                41,
                164,
                102,
                2,
                59
              ],
              "dst_ix_name": "marinade_deposit_stake_account",
              "dst_discriminator": [
                141,
                230,
                58,
                103,
                56,
                205,
                159,
                138
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_state",
                  "index": 0,
                  "writable": true,
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
                7,
                -1,
                8,
                -1,
                9,
                10,
                11,
                12,
                13,
                14,
                15,
                16
              ]
            },
            {
              "src_ix_name": "liquid_unstake",
              "src_discriminator": [
                30,
                30,
                119,
                240,
                191,
                227,
                12,
                16
              ],
              "dst_ix_name": "marinade_liquid_unstake",
              "dst_discriminator": [
                29,
                146,
                34,
                21,
                26,
                68,
                141,
                161
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
                7,
                8,
                9,
                -1,
                -1,
                10,
                11
              ]
            },
            {
              "src_ix_name": "order_unstake",
              "src_discriminator": [
                97,
                167,
                144,
                107,
                117,
                190,
                128,
                36
              ],
              "dst_ix_name": "marinade_order_unstake",
              "dst_discriminator": [
                202,
                3,
                33,
                27,
                183,
                156,
                57,
                231
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_state",
                  "index": 0,
                  "writable": true,
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
              "src_ix_name": "claim",
              "src_discriminator": [
                62,
                198,
                214,
                193,
                213,
                159,
                108,
                210
              ],
              "dst_ix_name": "marinade_claim",
              "dst_discriminator": [
                54,
                44,
                48,
                204,
                218,
                141,
                36,
                5
              ],
              "dynamic_accounts": [
                {
                  "name": "glam_state",
                  "index": 0,
                  "writable": true,
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
                8
              ]
            }
          ]
        }""";


    final var srcIxData = """
        [{
          "programId": "11111111111111111111111111111111",
          "accounts": [{
              "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
              "feePayer": false,
              "signer": true,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            }],
          "data": "AwAAAI4hVXVdrV4a/mWQikafyFE8XxSgQvDuF+bnvW+exKlqHAAAAAAAAAAyMDI1LTAzLTE0VDIxOjIzOjUyLjgyODM2MFp+gPAWAAAAAABYAAAAAAAAAAVF42W+8nGtdTUDZ1ZdpA2jNtwch5uxVIp6/MVaqTke"
        },
        {
          "programId": "MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD",
          "accounts":     [{
              "publicKey": "8szGkuLTAux9XMgZ2vtY39jVSowEcpBfFfD8hXSEqdGC",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "mSoLzYCxHdYgdzU16g5QSh3i5K3z3KZK7ytfqcJm7So",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "Hk5Js6PRDejQwWk164smpPtfymoKwP6iDjogYKozffhG",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "9AmBGCiDdiHSA7mspgFa6pwZ1PTfBj7DhF6nudpiXmNN",
              "feePayer": false,
              "signer": true,
              "writable": false,
              "invoked": false
            },{
              "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
              "feePayer": false,
              "signer": false,
              "writable": true,
              "invoked": false
            },{
              "publicKey": "SysvarC1ock11111111111111111111111111111111",
              "feePayer": false,
              "signer": false,
              "writable": false,
              "invoked": false
            },{
              "publicKey": "SysvarRent111111111111111111111111111111111",
              "feePayer": false,
              "signer": false,
              "writable": false,
              "invoked": false
            },{
              "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
              "feePayer": false,
              "signer": false,
              "writable": false,
              "invoked": false
            }],
          "data": "YaeQa3W+gCQA4fUFAAAAAA=="
        }]""";

    final var mappedIxData = """
        [
          {
            "programId": "11111111111111111111111111111111",
            "accounts": [
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": false,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              }
            ],
            "data": "AwAAAI4hVXVdrV4a/mWQikafyFE8XxSgQvDuF+bnvW+exKlqHAAAAAAAAAAyMDI1LTAzLTE0VDIxOjIzOjUyLjgyODM2MFp+gPAWAAAAAABYAAAAAAAAAAVF42W+8nGtdTUDZ1ZdpA2jNtwch5uxVIp6/MVaqTke"
          },
          {
            "programId": "GLAMbTqav9N9witRjswJ8enwp9vv5G8bsSJ2kPJ4rcyc",
            "accounts": [
              {
                "publicKey": "C8QQdekKRFRt5u78vo3YQcZTLWB5WkPhzpkwsKwjnNTo",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "9AmBGCiDdiHSA7mspgFa6pwZ1PTfBj7DhF6nudpiXmNN",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD",
                "feePayer": true,
                "signer": true,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "8szGkuLTAux9XMgZ2vtY39jVSowEcpBfFfD8hXSEqdGC",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "mSoLzYCxHdYgdzU16g5QSh3i5K3z3KZK7ytfqcJm7So",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "Hk5Js6PRDejQwWk164smpPtfymoKwP6iDjogYKozffhG",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "3NEcMhkxVNJMuE6x4F94iFMfyhpL4Bn9WAmVPxKs2YPn",
                "feePayer": false,
                "signer": false,
                "writable": true,
                "invoked": false
              },
              {
                "publicKey": "SysvarC1ock11111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "SysvarRent111111111111111111111111111111111",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              },
              {
                "publicKey": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
                "feePayer": false,
                "signer": false,
                "writable": false,
                "invoked": false
              }
            ],
            "data": "ygMhG7ecOecA4fUFAAAAAA=="
          }
        ]
        """;


    final var invokedProgram = PublicKey.fromBase58Encoded("GLAMbTqav9N9witRjswJ8enwp9vv5G8bsSJ2kPJ4rcyc");
    final var systemProgramProxies = createProxies(invokedProgram, systemProgramMappingJson);
    final var marinadeProgramProxies = createProxies(invokedProgram, marinadeMappingJson);
    final var programProxies = HashMap.<PublicKey, ProgramProxy<GlamVaultAccounts>>newHashMap(systemProgramProxies.size() + marinadeProgramProxies.size());
    for (final var proxy : systemProgramProxies) {
      programProxies.put(proxy.cpiProgram(), proxy);
    }
    for (final var proxy : marinadeProgramProxies) {
      programProxies.put(proxy.cpiProgram(), proxy);
    }
    final var txMapper = TransactionMapper.createMapper(programProxies);

    final var srcInstructions = parseInstructions(srcIxData);
    assertEquals(2, srcInstructions.size());
    var createTicketIx = srcInstructions.getFirst();
    final var marinadeProgram = PublicKey.fromBase58Encoded("MarBmsSgKXdrN1egZf5sqe1TMai9K1rChYNDJgjq7aD");
    var unstakeIx = srcInstructions.getLast();
    assertEquals(marinadeProgram, unstakeIx.programId().publicKey());

    final var stateAccount = PublicKey.fromBase58Encoded("C8QQdekKRFRt5u78vo3YQcZTLWB5WkPhzpkwsKwjnNTo");
    final var vaultAccount = PublicKey.fromBase58Encoded("9AmBGCiDdiHSA7mspgFa6pwZ1PTfBj7DhF6nudpiXmNN");
    final var vaultAccounts = new GlamVaultAccounts(
        AccountMeta.createRead(stateAccount),
        AccountMeta.createWrite(stateAccount),
        AccountMeta.createRead(vaultAccount),
        AccountMeta.createWrite(vaultAccount)
    );
    final var feePayer = AccountMeta.createFeePayer(PublicKey.fromBase58Encoded("AZpNg57C34kSTvFGVdgJuZ7sYvDTU3EGPNwNhQGqNXkD"));


    final var expectedInstructions = parseInstructions(mappedIxData);
    assertEquals(2, expectedInstructions.size());
    var mappedIx = expectedInstructions.getFirst();
    assertEquals(createTicketIx, mappedIx);

    final var mappedInstructions = txMapper.mapInstructions(feePayer, vaultAccounts, srcInstructions);
    assertEquals(2, mappedInstructions.length);
    mappedIx = mappedInstructions[0];
    assertEquals(createTicketIx, mappedIx);

    var mappedUnstakeIx = mappedInstructions[1];
    assertEquals(invokedProgram, mappedUnstakeIx.programId().publicKey());

    assertEquals(expectedInstructions, Arrays.asList(mappedInstructions));
  }

  private static List<Instruction> parseInstructions(final String ixData) {
    final var instructions = new ArrayList<Instruction>();
    final var ji = JsonIterator.parse(ixData);
    while (ji.readArray()) {
      final var programId = PublicKey.fromBase58Encoded(ji.skipUntil("programId").readString());
      final var accounts = new ArrayList<AccountMeta>();
      for (ji.skipUntil("accounts"); ji.readArray(); ) {
        final var parser = new AccountMetaParser();
        ji.testObject(parser);
        accounts.add(parser.createMeta());
      }
      final byte[] data = ji.skipUntil("data").decodeBase64String();
      final var ix = Instruction.createInstruction(programId, accounts, data);
      instructions.add(ix);
      ji.closeObj();
    }
    return instructions;
  }

  static final class AccountMetaParser implements FieldBufferPredicate {

    private PublicKey publicKey;
    private boolean feePayer;
    private boolean signer;
    private boolean writable;
    private boolean invoked;

    private AccountMetaParser() {
    }

    AccountMeta createMeta() {
      return AccountMeta.createMeta(
          publicKey,
          invoked,
          feePayer,
          writable,
          signer
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("publicKey", buf, offset, len)) {
        publicKey = PublicKey.fromBase58Encoded(ji.readString());
      } else if (fieldEquals("feePayer", buf, offset, len)) {
        feePayer = ji.readBoolean();
      } else if (fieldEquals("signer", buf, offset, len)) {
        signer = ji.readBoolean();
      } else if (fieldEquals("writable", buf, offset, len)) {
        writable = ji.readBoolean();
      } else if (fieldEquals("invoked", buf, offset, len)) {
        invoked = ji.readBoolean();
      } else {
        throw new IllegalStateException("Unexpected field: " + new String(buf, offset, len));
      }
      return true;
    }
  }
}

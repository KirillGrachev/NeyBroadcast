package eu.neydev.broadcast.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Экономика необязательна: сервис без провайдера остается работоспособным
 * для бесплатных каналов, а списание с провайдером проверяется по
 * EconomyResponse, а не «по факту вызова».
 */
class EconomyServiceTest {

    private final Player player = mock(Player.class);
    private final Economy economy = mock(Economy.class);

    private EconomyResponse response(EconomyResponse.ResponseType type, String error) {
        return new EconomyResponse(0.0, 0.0, type, error);
    }

    @Test
    @DisplayName("Без провайдера сервис выключен, но жив")
    void unavailableServiceStaysAlive() {

        EconomyService service = new EconomyService(null);

        assertFalse(service.isAvailable());
        assertEquals("none", service.getProviderName());
        assertFalse(service.has(player, 0.0), "без экономики денег нет ни у кого");

    }

    @Test
    @DisplayName("Списание без провайдера - честный отказ, а не исключение")
    void withdrawWithoutEconomyFails() {

        EconomyService.WithdrawResult result = new EconomyService(null).withdraw(player, 100.0);

        assertFalse(result.success());
        assertEquals("no economy provider is connected", result.error());

    }

    @Test
    @DisplayName("Провайдер виден по имени")
    void providerNameIsExposed() {

        when(economy.getName()).thenReturn("Essentials Economy");

        EconomyService service = new EconomyService(economy);

        assertTrue(service.isAvailable());
        assertEquals("Essentials Economy", service.getProviderName());

    }

    @Test
    @DisplayName("Баланс сравнивается с суммой, как в 1.x")
    void hasComparesBalance() {

        when(economy.getBalance(player)).thenReturn(1500.0);

        EconomyService service = new EconomyService(economy);

        assertTrue(service.has(player, 1500.0), "ровно хватает");
        assertTrue(service.has(player, 1499.99));
        assertFalse(service.has(player, 1500.01));

    }

    @Test
    @DisplayName("Успешное списание проходит")
    void successfulWithdrawalPasses() {

        when(economy.withdrawPlayer(player, 1500.0))
                .thenReturn(response(EconomyResponse.ResponseType.SUCCESS, null));

        EconomyService.WithdrawResult result = new EconomyService(economy).withdraw(player, 1500.0);

        assertTrue(result.success());
        assertEquals("", result.error());

    }

    @Test
    @DisplayName("Отклоненное списание несет текст ошибки провайдера")
    void failedWithdrawalCarriesProviderError() {

        when(economy.withdrawPlayer(player, 1500.0))
                .thenReturn(response(EconomyResponse.ResponseType.FAILURE, "account is frozen"));

        EconomyService.WithdrawResult result = new EconomyService(economy).withdraw(player, 1500.0);

        assertFalse(result.success());
        assertEquals("account is frozen", result.error());

    }

    @Test
    @DisplayName("Отклоненное списание без текста ошибки дает unknown error")
    void failedWithdrawalWithoutTextIsUnknown() {

        when(economy.withdrawPlayer(player, 10.0))
                .thenReturn(response(EconomyResponse.ResponseType.FAILURE, null));

        assertEquals("unknown error", new EconomyService(economy).withdraw(player, 10.0).error());

    }

    @Test
    @DisplayName("Null-ответ провайдера - отказ, а не NPE")
    void nullResponseIsFailure() {

        when(economy.withdrawPlayer(any(Player.class), anyDouble())).thenReturn(null);

        EconomyService.WithdrawResult result = new EconomyService(economy).withdraw(player, 10.0);

        assertFalse(result.success());
        assertEquals("the economy provider returned no response", result.error());

    }

    @Test
    @DisplayName("format отдает строку провайдера")
    void formatUsesProvider() {

        when(economy.format(1500.0)).thenReturn("$1,500.00");

        assertEquals("$1,500.00", new EconomyService(economy).format(1500.0));

    }

    @Test
    @DisplayName("format без провайдера возвращается к целому числу")
    void formatWithoutProviderFallsBackToWholeNumber() {

        assertEquals("1500", new EconomyService(null).format(1500.0));
        assertEquals("2000", new EconomyService(null).format(2000.75));

    }

    @Test
    @DisplayName("format переживает null и исключение провайдера")
    void formatSurvivesBrokenProviders() {

        when(economy.format(100.0)).thenReturn(null);
        assertEquals("100", new EconomyService(economy).format(100.0));

        when(economy.format(200.0)).thenThrow(new UnsupportedOperationException("not implemented"));
        assertEquals("200", new EconomyService(economy).format(200.0));

    }

}

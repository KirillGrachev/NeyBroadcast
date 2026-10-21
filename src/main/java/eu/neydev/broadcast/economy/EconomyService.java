package eu.neydev.broadcast.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Сервис экономики Vault.
 *
 * Экономика НЕОБЯЗАТЕЛЬНА: в 1.x отсутствие Vault или провайдера выключало
 * весь плагин (setEnabled(false)) - вместе с бесплатным каналом /bc,
 * которому деньги не нужны вовсе. Здесь сервис просто работает без
 * провайдера: бесплатные каналы доступны всегда, а платный канал с
 * неподключенной экономикой отказывает одному себе и объясняет почему.
 *
 * Списание проверяется по EconomyResponse: в 1.x результат
 * withdrawPlayer выбрасывался, и рассылка уходила даже тогда, когда
 * экономический плагин списание отклонил (игрок получал и товар, и деньги).
 */
public class EconomyService {

    private final @Nullable Economy economy;

    public EconomyService(@Nullable Economy economy) {
        this.economy = economy;
    }

    /**
     * Подключен ли экономический провайдер.
     */
    public boolean isAvailable() {
        return economy != null;
    }

    /**
     * Название провайдера для диагностики.
     *
     * @return имя экономического плагина или "none"
     */
    public @NotNull String getProviderName() {
        return economy == null ? "none" : economy.getName();
    }

    /**
     * Хватает ли игроку денег.
     *
     * Сравнение идет через getBalance, как в 1.x: метод has() некоторые
     * провайдеры реализуют недобросовестно, а баланс - факт.
     *
     * @param player плательщик
     * @param amount требуемая сумма
     * @return true если денег достаточно (без экономики - всегда false)
     */
    public boolean has(@NotNull OfflinePlayer player, double amount) {
        return economy != null && economy.getBalance(player) >= amount;
    }

    /**
     * Списывает деньги и проверяет ответ провайдера.
     *
     * @param player плательщик
     * @param amount сумма списания
     * @return результат с текстом ошибки провайдера при неудаче
     */
    public @NotNull WithdrawResult withdraw(@NotNull OfflinePlayer player, double amount) {

        if (economy == null) {
            return new WithdrawResult(false, "no economy provider is connected");
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);

        if (response == null) {
            return new WithdrawResult(false, "the economy provider returned no response");
        }

        if (response.transactionSuccess()) {
            return new WithdrawResult(true, "");
        }

        return new WithdrawResult(false,
                response.errorMessage == null ? "unknown error" : response.errorMessage);

    }

    /**
     * Форматирует сумму средствами экономического плагина (символ валюты,
     * разделители). Без провайдера или при его ошибке возвращается целое
     * число - тот же формат, что {count} в 1.x.
     *
     * @param amount сумма
     * @return отформатированная строка
     */
    public @NotNull String format(double amount) {

        if (economy == null) {
            return String.valueOf((int) amount);
        }

        try {

            String formatted = economy.format(amount);
            return formatted == null ? String.valueOf((int) amount) : formatted;

        } catch (RuntimeException exception) {
            // Недобросовестный провайдер бросает на format: сумма все равно
            // должна быть показана, целочисленный fallback лучше исключения
            return String.valueOf((int) amount);
        }

    }

    /**
     * Результат списания.
     *
     * @param success список прошел
     * @param error   текст ошибки провайдера (пустой при успехе)
     */
    public record WithdrawResult(boolean success, @NotNull String error) {
    }

}

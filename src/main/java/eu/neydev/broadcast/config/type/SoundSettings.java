package eu.neydev.broadcast.config.type;

import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

/**
 * Настройки звука канала рассылки.
 *
 * {@link Sound} разрешается из строки конфигурации ОДИН раз при загрузке
 * снапшота, а не на каждую команду: в 1.x неизвестное имя звука падало
 * IllegalArgumentException из Sound.valueOf прямо в обработчике команды -
 * опечатка в config.yml превращала каждую попытку рассылки в исключение
 * в консоли. Здесь недопустимое имя дает sound == null (менеджер
 * конфигурации печатает предупреждение), а воспроизведение такой
 * настройки просто пропускается.
 *
 * @param enabled включен ли звук канала
 * @param sound   разрешенный звук или null, если имя в конфигурации неизвестно
 * @param volume  громкость (0.0 и выше)
 * @param pitch   высота (0.0 и выше)
 */
public record SoundSettings(boolean enabled,
                            @Nullable Sound sound,
                            float volume,
                            float pitch) {

    /** Настройки по умолчанию: звук включен, как в поставляемом config.yml. */
    public static final boolean DEFAULT_ENABLED = true;

    /** Имя звука по умолчанию из поставляемого config.yml. */
    public static final String DEFAULT_TYPE = "BLOCK_NOTE_BLOCK_HAT";

    public static final float DEFAULT_VOLUME = 0.7f;

    public static final float DEFAULT_PITCH = 1.0f;

    /**
     * Можно ли воспроизводить звук: настройка включена и имя разрешено.
     */
    public boolean isPlayable() {
        return enabled && sound != null;
    }

}

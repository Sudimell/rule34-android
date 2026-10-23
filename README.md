# rule34-android

Unofficial Android client for browsing rule34.xxx. 18+ only. Not affiliated with the site, and not meant for Google Play.

Неофициальный клиент rule34.xxx. Вместо мобильного сайта: поиск по тегам, сетка, просмотр со свайпом и зумом, скачивание, локальные избранное и история, чёрный список тегов. Аккаунт сайта, комментарии и пулы в этой версии не подключаются.

Приложение для людей старше 18 лет. Сайт rule34.xxx и его владельцы к проекту отношения не имеют. В Google Play его нет: площадка ограничивает приложения с сексуальным контентом.

## Ключ API

С августа 2025 `api.rule34.xxx` отвечает только с личным ключом. Общий ключ в репозиторий класть нельзя: сайт запрещает больше одного ключа на аккаунт и отзывает такие ключи.

1. Войдите на https://rule34.xxx/
2. Откройте https://rule34.xxx/index.php?page=account&s=options
3. В блоке API Access Credentials возьмите `user id` и `api key`. Если ключа нет, отметьте Generate New Key и сохраните настройки.
4. Вставьте оба значения в приложение и нажмите «Проверить».

Ключ лежит в зашифрованных настройках устройства и не попадает в git. Условия API: https://api.rule34.xxx/. Лимит — около 60 запросов в минуту.

## Сборка

Нужны JDK 21 и Android SDK с `platforms;android-37.0` и `build-tools;36.0.0`.

```
./gradlew testDebugUnitTest assembleDebug
```

Готовый debug-APK: `app/build/outputs/apk/debug/app-debug.apk`. Подписанный release собирается локально, keystore в репозиторий не кладётся.

## Лицензия

MIT. См. [LICENSE](LICENSE).

# Chess Duel (Forge 1.20.1) — база

Сборка: нужен JDK 17 и Gradle 8.1.1+ (или gradle wrapper из Forge MDK 1.20.1-47.3.0).
  gradle build        -> build/libs/chessduel-0.1.0.jar  (кладётся в папку mods)
  gradle runClient    -> запуск Minecraft для теста прямо из проекта

В игре:
  J                      — окно настроек (доска / фигуры)
  /chessduel practice N  — тренировка без моба, N = 1..4
  удар по мобу / атака моба — начинается партия
  ПКМ по участнику дуэли (другим игроком) — наблюдение
  креатив + Shift + удар — обычный удар (убрать моба)

import re
import os

SCHEMA_FILE = '/home/Nariman/Projects/diplom/src/main/resources/db/migration/V1_CLEANED_SCHEMA.sql'
if not os.path.exists(SCHEMA_FILE):
    SCHEMA_FILE = '/home/Nariman/Projects/diplom/src/main/resources/db/migration/V1_CLEANED_SCHEMA'

OUT_DIR = '/home/Nariman/Projects/diplom/docs/database_tables'

TABLE_DESCRIPTIONS = {
    'app_user': 'Пользователи платформы',
    'role': 'Роли пользователей',
    'genre': 'Жанры новелл',
    'tag': 'Теги новелл',
    'novel': 'Литературные произведения (новеллы)',
    'novel_genre': 'Связь новелл и жанров',
    'novel_tag': 'Связь новелл и тегов',
    'chapter': 'Главы новелл',
    'chapter_blocks': 'Блоки контента (абзацы/изображения) внутри главы',
    'comment': 'Комментарии пользователей',
    'rating': 'Оценки новелл',
    'message': 'Личные сообщения между пользователями',
    'conversations': 'Беседы (диалоги и группы)',
    'conversation_members': 'Участники бесед',
    'user_followers': 'Подписчики',
    'user_friends': 'Друзья',
    'user_relations': 'Связи между пользователями',
    'user_library': 'Библиотека пользователя',
    'user_settings': 'Пользовательские настройки',
    'user_blocks': 'Блокировки (черный список)',
    'user_close_friends': 'Близкие друзья',
    'reading_history': 'История чтения',
    'verification_tokens': 'Токены верификации (email и др.)',
    'subscribable_entities': 'Сущности, на которые можно подписаться',
    'forum_topic': 'Темы на форуме',
    'channel': 'Пользовательские каналы',
    'channel_post': 'Посты в каналах',
    'user_subscriptions': 'Подписки пользователей',
    'notifications': 'Уведомления',
    'audit_log': 'Журнал аудита действий (логи)',
    'user_sessions': 'Активные сессии пользователей',
    'flyway_schema_history': 'Служебная таблица миграций Flyway'
}

with open(SCHEMA_FILE, 'r', encoding='utf-8') as f:
    sql_content = f.read()

# Извлечение таблиц
tables_data = {}
# Ищем блоки CREATE TABLE
create_table_pattern = re.compile(r'CREATE TABLE \w+\.?(\w+)\s*\((.*?)\);', re.IGNORECASE | re.DOTALL)

for match in create_table_pattern.finditer(sql_content):
    table_name = match.group(1)
    columns_raw = match.group(2)
    
    columns = []
    # Разбиваем по запятым, игнорируя запятые внутри скобок (например DECIMAL(3, 2))
    # Проще разбить по строкам, так как pg_dump генерирует колонки построчно
    lines = columns_raw.split('\n')
    for line in lines:
        line = line.strip()
        if not line or line.startswith('CONSTRAINT') or line.startswith('UNIQUE') or line.startswith('PRIMARY KEY') or line.startswith('FOREIGN KEY') or line.startswith('CHECK'):
            continue
        
        # Парсим колонку. Обычно: `id bigint NOT NULL` или `title character varying(255)`
        parts = line.split()
        if len(parts) >= 2:
            col_name = parts[0]
            # Убираем запятую на конце
            if col_name.endswith(','):
                col_name = col_name[:-1]
                
            line_no_name = line[len(col_name):].strip().rstrip(',')
            col_type = parts[1]
            if len(parts) > 2 and parts[1].lower() in ['character', 'timestamp', 'double']:
                col_type = parts[1] + " " + parts[2]
                
            is_required = 'Да' if 'NOT NULL' in line.upper() else 'Нет'
            if 'PRIMARY KEY' in line.upper():
                is_required = 'Да (PK)'
                
            columns.append({
                'name': col_name,
                'type': col_type.replace(',', ', '),
                'required': is_required,
                'raw': line_no_name
            })
            
    tables_data[table_name] = columns

for t_name, cols in tables_data.items():
    desc = TABLE_DESCRIPTIONS.get(t_name, 'Хранение данных сущности ' + t_name)
    
    md_lines = []
    md_lines.append(f"# Таблица: `{t_name}`\n")
    md_lines.append(f"**Назначение:** {desc}\n")
    md_lines.append("## Структура\n")
    md_lines.append("| Наименование поля | Тип данных | Обязательность | Описание / Примечание |")
    md_lines.append("|---|---|---|---|")
    
    for c in cols:
        name = c['name']
        ctype = c['type'].upper()
        req = c['required']
        # Простое описание
        note = ''
        if name == 'id':
            note = 'Уникальный идентификатор'
        elif 'id' in name:
            note = f"Ссылка на сущность (ID)"
        elif name.endswith('_at') or name.endswith('_date') or name == 'timestamp':
            note = 'Дата и время'
        elif name.startswith('is_') or name.startswith('can_'):
            note = 'Флаг (да/нет)'
            
        md_lines.append(f"| `{name}` | `{ctype}` | {req} | {note} |")
        
    with open(os.path.join(OUT_DIR, f"{t_name}.md"), 'w', encoding='utf-8') as f:
        f.write('\n'.join(md_lines))

print(f"Generated {len(tables_data)} files.")

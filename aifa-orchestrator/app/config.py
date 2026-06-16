from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    aifa_api_base_url: str = "http://localhost:8080"
    redis_url: str = "redis://localhost:6379"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "mistral"
    ai_daily_limit: int = 20
    enable_llm: bool = True
    cors_origins: str = "*"


settings = Settings()

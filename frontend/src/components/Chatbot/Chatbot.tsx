import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Chatbot.module.css";
import {
  sendChatMessage,
  INITIAL_BOT_MESSAGES,
  type ChatMessage,
  type ChatHistoryItem,
  type ChatProduct,
} from "../../services/chatbot";

let messageCounter = 0;

function createChatMessage(
  sender: "user" | "bot",
  text: string,
  suggestions?: string[],
  products?: ChatProduct[]
): ChatMessage {
  messageCounter += 1;
  const now = new Date();
  return {
    id: `${sender}-${messageCounter}-${now.getTime()}`,
    sender,
    text,
    timestamp: now.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    suggestions,
    products,
  };
}

export function Chatbot() {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>(INITIAL_BOT_MESSAGES);
  const [inputValue, setInputValue] = useState("");
  const [isTyping, setIsTyping] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen, messages, isTyping]);

  const handleSend = async (customText?: string) => {
    const textToSend = (customText || inputValue).trim();
    if (!textToSend || isTyping) return;

    const userMessage = createChatMessage("user", textToSend);

    setMessages((prev) => [...prev, userMessage]);
    setInputValue("");
    setIsTyping(true);

    try {
      const history: ChatHistoryItem[] = [];
      let currentQuestion: string | null = null;

      for (const m of messages) {
        if (m.id === "welcome-1") continue;
        if (m.sender === "user") {
          currentQuestion = m.text;
        } else if (m.sender === "bot" && currentQuestion) {
          history.push({
            user: currentQuestion,
            answer: m.text,
          });
          currentQuestion = null;
        }
      }

      const recentHistory = history.slice(-6);
      const botReply = await sendChatMessage(textToSend, recentHistory);
      setMessages((prev) => [...prev, botReply]);
    } catch (err: unknown) {
      console.error("Chatbot send error:", err);
      let errMsg = "Xin lỗi, đã xảy ra lỗi kết nối. Vui lòng kiểm tra lại backend và thử lại!";
      if (err instanceof Error && err.message) {
        errMsg = `Lỗi kết nối: ${err.message}`;
      }
      const errorMessage = createChatMessage("bot", errMsg);
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleResetChat = () => {
    setMessages(INITIAL_BOT_MESSAGES);
  };

  const renderInlineMarkdown = (text: string) => {
    const tokens = text.split(/(\[.*?\]\(.*?\)|\*\*.*?\*\*|\*.*?\*)/g);

    return tokens.map((token, i) => {
      const linkMatch = token.match(/^\[(.*?)\]\((.*?)\)$/);
      if (linkMatch) {
        const [, label, url] = linkMatch;
        return (
          <a
            key={i}
            href={url}
            className={styles.chatLink}
            onClick={(e) => {
              e.preventDefault();
              if (url.startsWith("/")) {
                navigate(url);
              } else {
                window.open(url, "_blank");
              }
            }}
          >
            {label}
            <i className="fi fi-rr-arrow-up-right-from-square" style={{ fontSize: "10px", marginLeft: "2px" }} />
          </a>
        );
      }

      if (token.startsWith("**") && token.endsWith("**") && token.length >= 4) {
        return <strong key={i}>{token.slice(2, -2)}</strong>;
      }

      if (token.startsWith("*") && token.endsWith("*") && token.length >= 2) {
        return <em key={i}>{token.slice(1, -1)}</em>;
      }

      return token;
    });
  };

  const renderFormattedText = (rawText: string) => {
    return rawText.split("\n").map((line, idx) => {
      const trimmed = line.trim();
      const isBullet = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ");
      const displayLine = isBullet ? trimmed.replace(/^[-*•]\s*/, "• ") : line;

      return (
        <span
          key={idx}
          style={{
            display: "block",
            minHeight: line === "" ? "6px" : undefined,
            paddingLeft: isBullet ? "12px" : undefined,
          }}
        >
          {renderInlineMarkdown(displayLine)}
        </span>
      );
    });
  };

  return (
    <>
      <button
        type="button"
        className={styles.floatingTrigger}
        onClick={() => setIsOpen(!isOpen)}
        title={isOpen ? "Thu nhỏ trợ lý ảo" : "Mở trợ lý ảo"}
        aria-label="Trợ lý ảo"
      >
        <span className={styles.triggerIcon}>
          <i className={isOpen ? "fi fi-rr-cross" : "fi fi-rr-comment-alt"} />
        </span>
      </button>

      {isOpen && (
        <div className={styles.chatWindow}>
          <div className={styles.chatHeader}>
            <div className={styles.botProfile}>
              <div className={styles.avatarBox}>
                <i className="fi fi-rr-headset" />
              </div>
              <div>
                <div className={styles.botTitle}>Trợ lý Kho Hàng</div>
                <div className={styles.botSubtitle}>Hỗ trợ trực tuyến</div>
              </div>
            </div>

            <div className={styles.headerActions}>
              <button
                type="button"
                className={styles.headerBtn}
                onClick={handleResetChat}
                title="Làm mới cuộc trò chuyện"
              >
                <i className="fi fi-rr-refresh" />
              </button>
              <button
                type="button"
                className={styles.headerBtn}
                onClick={() => setIsOpen(false)}
                title="Đóng cửa sổ"
              >
                <i className="fi fi-rr-cross" />
              </button>
            </div>
          </div>
          <div className={styles.messagesList}>
            {messages.map((msg) => {
              const isUser = msg.sender === "user";
              return (
                <div
                  key={msg.id}
                  className={`${styles.messageRow} ${
                    isUser ? styles.userRow : styles.botRow
                  }`}
                >
                  {!isUser && (
                    <div className={styles.messageAvatar}>
                      <i className="fi fi-rr-headset" />
                    </div>
                  )}

                  <div style={{ display: "flex", flexDirection: "column", maxWidth: "100%" }}>
                    <div
                      className={`${styles.bubble} ${
                        isUser ? styles.userBubble : styles.botBubble
                      }`}
                    >
                      {renderFormattedText(msg.text)}

                      {!isUser && msg.products && msg.products.length > 0 && (
                        <div className={styles.productsContainer}>
                          {msg.products.map((p) => {
                            const imgSource = p.url || p.imageUrl;
                            return (
                              <div
                                key={p.id}
                                className={styles.productCard}
                                onClick={() => navigate(`/products/${p.id}`)}
                                title="Bấm để xem chi tiết sản phẩm"
                              >
                                <div className={styles.productThumb}>
                                  {imgSource ? (
                                    <img
                                      src={imgSource}
                                      alt={p.name}
                                      onError={(e) => {
                                        e.currentTarget.style.display = "none";
                                        const sibling = e.currentTarget.nextElementSibling;
                                        if (sibling) sibling.classList.remove(styles.hidden);
                                      }}
                                    />
                                  ) : null}
                                  <div className={`${styles.placeholderThumb} ${imgSource ? styles.hidden : ""}`}>
                                    <i className="fi fi-rr-shirt" />
                                  </div>
                                </div>

                                <div className={styles.productInfo}>
                                  <div className={styles.productName}>{p.name}</div>
                                  <div className={styles.productMeta}>
                                    <span className={styles.productCode}>{p.code}</span>
                                    {p.category && (
                                      <span className={styles.productCategory}>{p.category}</span>
                                    )}
                                  </div>
                                  <div className={styles.productStock}>
                                    <span>Tồn:</span>
                                    <strong className={p.totalStock <= 20 ? styles.stockLow : styles.stockOk}>
                                      {p.totalStock.toLocaleString()} sp
                                    </strong>
                                  </div>
                                </div>

                                <button
                                  type="button"
                                  className={styles.viewProductBtn}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    navigate(`/products/${p.id}`);
                                  }}
                                  title="Xem chi tiết"
                                >
                                  <i className="fi fi-rr-eye" />
                                  <span>Xem</span>
                                </button>
                              </div>
                            );
                          })}
                        </div>
                      )}

                      {!isUser && msg.suggestions && msg.suggestions.length > 0 && (
                        <div className={styles.suggestionsContainer}>
                          {msg.suggestions.map((sug, sIdx) => (
                            <button
                              key={sIdx}
                              type="button"
                              className={styles.suggestionPill}
                              onClick={() => handleSend(sug)}
                            >
                              <i className="fi fi-rr-arrow-small-right" />
                              <span>{sug}</span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                    <div className={styles.messageTime}>{msg.timestamp}</div>
                  </div>
                </div>
              );
            })}

            {isTyping && (
              <div className={`${styles.messageRow} ${styles.botRow}`}>
                <div className={styles.messageAvatar}>
                  <i className="fi fi-rr-headset" />
                </div>
                <div className={styles.typingIndicator}>
                  <span className={styles.typingDot} />
                  <span className={styles.typingDot} />
                  <span className={styles.typingDot} />
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          <div className={styles.chatFooter}>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSend();
              }}
              className={styles.inputWrapper}
            >
              <input
                ref={inputRef}
                type="text"
                className={styles.chatInput}
                placeholder="Nhập câu hỏi... (VD: tồn kho, đơn PO)"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
              />
              <button
                type="submit"
                className={styles.sendBtn}
                disabled={!inputValue.trim() || isTyping}
                title="Gửi"
              >
                <i className="fi fi-rr-paper-plane" />
              </button>
            </form>
          </div>
        </div>
      )}
    </>
  );
}

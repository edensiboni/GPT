import requests
from bs4 import BeautifulSoup
import sqlite3

from main import app


def scrape_search_results(query):
    url = "https://www.amazon.com/s?k=" + query
    response = requests.get(url)
    soup = BeautifulSoup(response.content, "html.parser")

    results = []
    for item in soup.select(".s-result-item"):
        name = item.select_one(".s-result-item .s-image-square-aspect img")["alt"]
        image = item.select_one(".s-result-item .s-image-square-aspect img")["src"]
        results.append({"name": name, "image": image})

    return results


def convert_currency(from_currency, to_currency, amount):
    api_key = "YOUR_API_KEY" # replace with your API key
    url = f"https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency={from_currency}" \
          f"&to_currency={to_currency}&apikey={api_key}"
    response = requests.get(url)
    data = response.json()
    exchange_rate = float(data["Realtime Currency Exchange Rate"]["5. Exchange Rate"])
    converted_amount = float(amount) * exchange_rate
    return round(converted_amount, 2)


@app.get("/searches")
async def get_searches():
    conn = sqlite3.connect("database.db")
    cursor = conn.cursor()
    cursor.execute("SELECT query, time, item_name, amazon_com_price, amazon_uk_price, amazon_de_price, amazon_ca_price FROM searches")
    rows = cursor.fetchall()
    conn.close()

    # Scrape prices for each query in the database
    for row in rows:
        query = row[0]
        item_name = row[2]
        rating = row[3]
        scrape_prices(query, item_name, rating)

    return "Prices scraped."


def scrape_prices(query, item_name, rating, headers):
    url = "https://www.amazon.com/dp/" + query
    response = requests.get(url)
    soup = BeautifulSoup(response.content, "html.parser")

    asin = soup.select_one("#ASIN")["value"]

    amazon_com_price = soup.select_one("#priceblock_ourprice")
    if amazon_com_price:
        amazon_com_price = amazon_com_price.get_text()
        amazon_com_price_usd = convert_currency("USD", "USD", amazon_com_price)
    else:
        amazon_com_price_usd = "Not found"

    url_uk = "https://www.amazon.co.uk/dp/" + asin
    response_uk = requests.get(url_uk, headers=headers)
    soup_uk = BeautifulSoup(response_uk.content, "html.parser")

    amazon_co_uk_price = soup_uk.select_one("#priceblock_ourprice")
    if amazon_co_uk_price:
        amazon_co_uk_price = amazon_co_uk_price.get_text()
        amazon_uk_price_usd = convert_currency("GBP", "USD", amazon_co_uk_price)
        item_name = soup.select_one("#productTitle").get_text().strip()

    # Get the rating from the page
    rating = soup.select_one("i.a-icon-star span.a-icon-alt").get_text().split(" ")[0]

    # Build the Amazon URLs for each region
    url_com = f"https://www.amazon.com/dp/{asin}"
    url_uk = f"https://www.amazon.co.uk/dp/{asin}"
    url_de = f"https://www.amazon.de/dp/{asin}"
    url_ca = f"https://www.amazon.ca/dp/{asin}"

    # Save the item name and search query to the database
    conn = sqlite3.connect("database.db")
    cursor = conn.cursor()
    cursor.execute("INSERT INTO searches (query, time, item_name) VALUES (?, datetime('now'), ?)",
                    (query, item_name))
    conn.commit()
    conn.close()

    # Scrape the prices for each region and save to the database
    scrape_prices(query)

    # Build the response table
    response_table = f"""
        <table>
            <tr>
                <th>Item</th>
                <th>Rating</th>
                <th>Amazon.com</th>
                <th>Amazon.co.uk</th>
                <th>Amazon.de</th>
                <th>Amazon.ca</th>
            </tr>
            <tr>
                <td>{item_name}</td>
                <td>{rating}</td>
                <td><a href="{url_com}" target="_blank">{amazon_com_price_usd}</a></td>
                <td><a href="{url_uk}" target="_blank">{amazon_uk_price_usd}</a></td>
                
            </tr>
        </table>
        """

    return response_table

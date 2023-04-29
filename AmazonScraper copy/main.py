from flask import Flask, render_template, request, jsonify
import sqlite3
import asyncio
import aiohttp
import json
from bs4 import BeautifulSoup


app = Flask(__name__)


conn = sqlite3.connect('database.db')
print('Database created')
conn.execute('CREATE TABLE IF NOT EXISTS searches (id INTEGER PRIMARY KEY AUTOINCREMENT, keyword TEXT, asin TEXT, rating TEXT, price_usd TEXT, price_ca TEXT, price_uk TEXT, price_de TEXT)')
print('Table created')


conn.close()



@app.route('/')
def index():
    return render_template('index.html')


@app.route('/search')
def search():
    query = request.args.get('query')
    if query:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        results = loop.run_until_complete(get_search_results(query))
        loop.close()
        return jsonify(results)
    return jsonify([])

app.run()

async def get_search_results(query):
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36'
    }
    async with aiohttp.ClientSession(headers=headers) as session:
        url = f'https://www.amazon.com/s?k={query}'
        async with session.get(url) as response:
            html = await response.text()
    soup = BeautifulSoup(html, 'html.parser')
    results = []
    for item in soup.select('div[data-asin]')[:10]:
        name = item.select_one('span.a-text-normal').text
        image = item.select_one('img.s-image')['src']
        results.append({'name': name, 'image': image})
    return results


async def get_product_details(asin):
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36'
    }
    async with aiohttp.ClientSession(headers=headers) as session:
        url = f'https://www.amazon.com/dp/{asin}'
        async with session.get(url) as response:
            html = await response.text()
    soup = BeautifulSoup(html, 'html.parser')
    name = soup.select_one('#productTitle').text.strip()
    rating = soup.select_one('span.a-icon-alt').text.split()[0]
    return {'name': name, 'rating': rating}

async def get_product_prices(name, asin):
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36'
    }
    prices = {'Amazon.com': '', 'Amazon.co.uk': '', 'Amazon.de': '', 'Amazon.ca': ''}
    async with aiohttp.ClientSession(headers=headers) as session:
        # search on Amazon.co.uk
        url = f'https://www.amazon.co.uk/s?k={name}'
        async with session.get(url) as response:
            html = await response.text()
        soup = BeautifulSoup(html, 'html.parser')
        asin_link = soup.select_one(f'a[href*="/dp/{asin}"]')
        if asin_link:
            price = soup.select_one(f'span[data-asin="{asin}"] span.a-price span.a-offscreen')
            if price:
                prices['Amazon.co.uk'] = price.text.strip()
        else:
            product_links = soup.select('a.a-link-normal.a-text-normal')
            for link in product_links:
                product_name = link.select_one('span.a-text-normal').text
                if name.lower() in product_name.lower():
                    url = 'https://www.amazon.co.uk' + link['href']
                    async with session.get(url) as response:
                        html = await response.text()
                    soup = BeautifulSoup(html, 'html.parser')
                    price = soup.select_one('span#priceblock_ourprice')
                    if price:
                        prices['Amazon.co.uk'] = price.text.strip()
                    break

        # search on Amazon.de
        url = f'https://www.amazon.de/s?k={name}'
        async with session.get(url) as response:
            html = await response.text()
        soup = BeautifulSoup(html, 'html.parser')
        asin_link = soup.select_one(f'a[href*="/dp/{asin}"]')
        if asin_link:
            price = soup.select_one(f'span[data-asin="{asin}"] span.a-price span.a-offscreen')
            if price:
                prices['Amazon.de'] = price.text.strip()
        else:
            product_links = soup.select('a.a-link-normal.a-text-normal')
            for link in product_links:
                product_name = link.select_one('span.a-text-normal').text
                if name.lower() in product_name.lower():
                    url = 'https://www.amazon.de' + link['href']
                    async with session.get(url) as response:
                        html = await response.text()
                    soup = BeautifulSoup(html, 'html.parser')
                    price = soup.select_one('span#priceblock_ourprice')
                    if price:
                        prices['Amazon.de'] = price.text.strip()
                    break

        # search on Amazon.ca
        url = f'https://www.amazon.ca/s?k={name}'
        async with session.get(url) as response:
            html = await response.text()
        soup = BeautifulSoup(html, 'html.parser')
        asin_link = soup.select_one(f'a[href*="/dp/{asin}"]')
        if asin_link:
            price = soup.select_one(f'span[data-asin="{asin}"] span.a-price span.a-offscreen')
            if price:
                prices['Amazon.ca'] = price.text.strip()
        else:
            product_links = soup.select('a.a-link-normal.a-text-normal')
            for link in product_links:
                product_name = link.select_one('span.a-text-normal').text
                if name.lower() in product_name.lower():
                    url = 'https://www.amazon.ca' + link['href']
                    async with session.get(url) as response:
                        html = await response.text()
                    soup = BeautifulSoup(html, 'html.parser')
                    price = soup.select_one('span#priceblock_ourprice')
                    if price:
                        prices['Amazon.ca'] = price.text.strip()
                    break

        return prices


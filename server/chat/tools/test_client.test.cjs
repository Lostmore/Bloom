const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const html = fs.readFileSync(require('node:path').join(__dirname, 'test_client.html'), 'utf8');

// A DOM stub that fails immediately if rendering attempts to interpret HTML.
class Element {
    constructor(tag) { this.tag = tag; this.children = []; this.textContent = ''; }
    set innerHTML(_) { throw new Error('HTML parsing is forbidden'); }
    appendChild(child) { this.children.push(child); }
    replaceChildren() { this.children = []; }
}
const chat = new Element('div');
const context = vm.createContext({
    URL,
    window: { location: { host: 'bloom.test', protocol: 'https:', origin: 'https://bloom.test' } },
    document: {
        createElement: tag => new Element(tag),
        createTextNode: text => ({ tag: '#text', textContent: text }),
        getElementById: () => chat
    }
});
vm.runInContext(html.match(/<script>([\s\S]*?)<\/script>/)[1], context);

test('message and sender markup remain literal text; unsafe attachments cannot create links', () => {
    const payload = '<img src=x onerror=alert(1)>';
    context.printMsg(payload, payload, '', ['javascript:alert(1)', 'https://evil.test/file']);
    const message = chat.children.at(-1);
    assert.equal(message.children[0].textContent, payload + ': ');
    assert.equal(message.children[1].textContent, payload);
    assert.equal(message.children[2].children.length, 2);
    assert.ok(message.children[2].children.every(item => item.children.length === 0));
    assert.doesNotMatch(html, /\.innerHTML\s*=/);
});

test('only same-origin UUID media routes are accepted, without query or credentials', () => {
    const path = '/api/v1/media/12345678-1234-1234-1234-123456789abc';
    assert.equal(context.safeMediaUrl(path), 'https://bloom.test' + path);
    for (const value of ['javascript:alert(1)', 'data:text/html,test', '//evil.test' + path,
        'https://user@bloom.test' + path, path + '?token=x', path + '#x', '/api/v1/auth/me',
        path + '\" onclick=alert(1)', null]) {
        assert.equal(context.safeMediaUrl(value), null);
    }
    context.printMsg('user', 'photo', '', [path]);
    assert.equal(chat.children.at(-1).children[2].children[0].children[0].tag, 'button');
});
